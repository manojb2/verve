package com.verve.verve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;

import static com.verve.verve.Constants.FAILED_RESPONSE;
import static com.verve.verve.Constants.OK_RESPONSE;


@RestController
@RequestMapping("/api/verve")
public class RequestController {


    private static final Logger logger = LoggerFactory.getLogger(RequestController.class);

    @Autowired
    private RequestService requestService;


    @GetMapping("/accept")
    public ResponseEntity<String> acceptRequest(@RequestParam int id,
                                                @RequestParam(required = false) String endpoint) {
        try {
            boolean isUnique = requestService.processRequest(id, endpoint);
            return ResponseEntity.ok(OK_RESPONSE);

        } catch (Exception e) {
            logger.error("Failed to process request with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(FAILED_RESPONSE);
        }

    }
}