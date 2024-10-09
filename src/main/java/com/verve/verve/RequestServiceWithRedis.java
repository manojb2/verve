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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.Objects;

import java.util.concurrent.*;


import static com.verve.verve.Constants.*;

@Service
public class RequestServiceWithRedis {


    private static final Logger logger = LoggerFactory.getLogger(RequestServiceWithRedis.class);

    private final CloseableHttpClient httpClient;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    public RequestServiceWithRedis() {
        this.httpClient = HttpClients.createDefault();
    }


    public boolean processRequest(int id, String endpoint) {
        String currentMinuteKey = UNIQUE_IDS + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        boolean isUnique = Objects.requireNonNull(redisTemplate.opsForSet().add(currentMinuteKey, id)) == 1;

        if (isUnique) {
            logger.info("Received a unique request with id: {}", id);
        }

        if (endpoint != null && !endpoint.isEmpty()) {
            executorService.submit(() -> {
                try {
//                    sendHttpRequest(endpoint); get request
                    sendHttpPostRequest(endpoint, Objects.requireNonNull(redisTemplate.opsForSet().size(currentMinuteKey)).intValue());
                } catch (IOException e) {
                    logger.error("Exception while sending HTTP POST request: {}", e.getMessage());
                }
            });
        }

        return isUnique;
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
        String url = endpoint;
        HttpPost postRequest = new HttpPost(url);

        String jsonPayload = String.format("{\"uniqueCount\": %d}", uniqueCount);
        StringEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
        postRequest.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
            int statusCode = response.getCode();
            logger.info("HTTP POST request to {} returned status code {}", endpoint, statusCode);
        }
    }

    @Scheduled(fixedRate = LOGGING_TIME_INTERVAL)
    private void logCurrentMinuteRequestCount() {
        String currentMinuteKey = UNIQUE_IDS + LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
        Long uniqueCount = redisTemplate.opsForSet().size(currentMinuteKey);

        if (uniqueCount != null) {
            logger.info("Unique requests in minute {}: {}", currentMinuteKey, uniqueCount);
        }
        cleanUpOldCounts();
    }

    private void cleanUpOldCounts() {
        LocalDateTime now = LocalDateTime.now();

        String lastKey = UNIQUE_IDS + now.minusMinutes(LOGS_STORAGE_TIME).truncatedTo(ChronoUnit.MINUTES);

        boolean deleted = Boolean.TRUE.equals(redisTemplate.delete(lastKey));
        if (deleted) {
            logger.info("Deleted unique requests set for {}", lastKey);
        }
        else{
            logger.error("Error while deleting unique requests set for {}", lastKey);
        }
    }


}