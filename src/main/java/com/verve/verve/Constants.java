package com.verve.verve;

public final class Constants {

    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Cannot instantiate Constants class");
    }

    // Thread pool constants
    public static final int THREAD_POOL_SIZE = 10; // Number of threads in the pool

    // Other application constants
    public static final String OK_RESPONSE = "ok";
    public static final String FAILED_RESPONSE = "failed";

    // Time constants
    public static final long AWAIT_TERMINATION_TIMEOUT = 60; // Timeout in seconds
    public static final int LOGGING_TIME_INTERVAL = 60000;

    public static final String UNIQUE_IDS = "unique-ids:";

    public static final int LOGS_STORAGE_TIME = 10;
}