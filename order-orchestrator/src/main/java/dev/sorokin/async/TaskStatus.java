package dev.sorokin.async;


public enum TaskStatus {
    NEW,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_NON_RETRYABLE
}
