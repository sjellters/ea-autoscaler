package com.uni.ea_autoscaler.evaluation.exception;

public class EvaluationRetryableException extends RuntimeException {

    public EvaluationRetryableException(String message) {
        super(message);
    }

    public EvaluationRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
