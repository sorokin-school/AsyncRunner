package dev.sorokin.async;

public enum TaskExecutionStatus {
    SUCCESS,
    RETRYABLE_ERROR,
    NON_RETRYABLE_ERROR
}
