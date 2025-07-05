package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.core.utils.ThreadUtils;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class KubernetesRetryExecutor {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_DELAY_MS = 2000;

    public <T> T executeWithRetry(String operationName, ThrowingSupplier<T> action) {
        return executeWithRetry(operationName, action, DEFAULT_MAX_RETRIES, DEFAULT_DELAY_MS);
    }

    public <T> T executeWithRetry(String operationName, ThrowingSupplier<T> action, int maxRetries, long delayMs) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (Exception ex) {
                if (!isRetryable(ex) || attempt >= maxRetries) {
                    log.error("❌ Operation '{}' failed after {} retries", operationName, attempt, ex);
                    throw new RuntimeException("Kubernetes operation failed: " + operationName, ex);
                }
                attempt++;
                log.warn("🔁 Retrying '{}': attempt {} after {} ms – error: {}", operationName, attempt, delayMs, ex.getMessage());
                ThreadUtils.sleep(delayMs);
            }
        }
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof IOException) return true;

        if (ex instanceof ApiException apiEx) {
            int code = apiEx.getCode();
            return (code == 404 || code == 409 || code >= 500);
        }

        return false;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
