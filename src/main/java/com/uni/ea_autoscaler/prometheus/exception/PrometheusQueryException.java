package com.uni.ea_autoscaler.prometheus.exception;

public class PrometheusQueryException extends RuntimeException {

    public PrometheusQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}

