package com.verve.verve;



import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static com.verve.verve.Constants.*;

@Service
public class RequestService {

    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    // Map to hold sets of unique request IDs for each minute
    private final ConcurrentHashMap<String, Set<Integer>> requestMap = new ConcurrentHashMap<>();

    private final CloseableHttpClient httpClient;

    @Autowired
    public RequestService() {
        this.httpClient = HttpClients.createDefault();
    }
    @Autowired
    private ExecutorService executorService;

    public boolean processRequest(int id, String endpoint) {
        String currentMinuteKey = UNIQUE_IDS + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Create or retrieve the set for the current minute key
        Set<Integer> currentMinuteSet = requestMap.computeIfAbsent(currentMinuteKey, k -> ConcurrentHashMap.newKeySet());

        // Attempt to add the ID to the set
        boolean isUnique = currentMinuteSet.add(id);

        if (isUnique) {
            logger.info("Received a unique request with id: {}", id);
        }

        if (endpoint != null && !endpoint.isEmpty()) {
            executorService.submit(() -> {
                try {
                    sendHttpRequest(endpoint, requestMap.get(currentMinuteKey).size());
//                    sendHttpPostRequest(endpoint, requestMap.get(currentMinuteKey).size());
                } catch (IOException e) {
                    logger.error("Exception while sending HTTP POST request: {}", e.getMessage());
                }
            });
        }
        return isUnique;
    }

    @Scheduled(fixedRate = LOGGING_TIME_INTERVAL)
    private void logCurrentMinuteRequestCount() {
        String currentMinuteKey = UNIQUE_IDS + LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Set<Integer> previousMinuteSet = requestMap.get(currentMinuteKey);

        if (previousMinuteSet != null) {
            logger.info("Unique requests in minute {}: {}", currentMinuteKey, previousMinuteSet.size());
        }
        cleanUpOldCounts();
    }

    private void cleanUpOldCounts() {
        LocalDateTime now = LocalDateTime.now();
        String lastKey = UNIQUE_IDS + now.minusMinutes(LOGS_STORAGE_TIME).truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Remove the old minute's unique request set if it exists
        requestMap.remove(lastKey);
        logger.info("Cleaned up unique requests for {}", lastKey);
    }


    private void sendHttpRequest(String endpoint,  int uniqueCount) throws IOException {
        String url = endpoint + "?uniqueCount=" + uniqueCount;
        BasicHttpContext httpCtx = new BasicHttpContext();
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(null,request,httpCtx)) {
            int statusCode = response.getCode();
            logger.info("HTTP request to {} returned status code {}", endpoint, statusCode);
        }
    }
    private void sendHttpPostRequest(String endpoint, int uniqueCount) throws IOException {
        HttpPost postRequest = new HttpPost(endpoint);
        String jsonPayload = String.format("{\"uniqueCount\": %d}", uniqueCount);
        StringEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
        postRequest.setEntity(entity);
        try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
            int statusCode = response.getCode();
            logger.info("HTTP POST request to {} returned status code {}", endpoint, statusCode);
        }
    }
}
