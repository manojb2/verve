package com.verve.verve;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.verve.verve.Constants.AWAIT_TERMINATION_TIMEOUT;
import static com.verve.verve.Constants.THREAD_POOL_SIZE;

@SpringBootApplication
@EnableScheduling
public class VerveApplication {

	private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE); // Limit to 10 concurrent threads


	public static void main(String[] args) {
		SpringApplication.run(VerveApplication.class, args);
	}
	@Bean
	public ExecutorService executorService() {
		return executorService;
	}

	@PreDestroy
	public void shutdownExecutorService() {
		// Shutdown the executor service gracefully
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
				executorService.shutdownNow(); // Force shutdown if not terminated
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow(); // Handle interrupted exceptions
			Thread.currentThread().interrupt(); // Restore the interrupted status
		}
	}
}