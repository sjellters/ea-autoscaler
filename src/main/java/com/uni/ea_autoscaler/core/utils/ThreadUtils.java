package com.uni.ea_autoscaler.core.utils;

public class ThreadUtils {

    private ThreadUtils() {
        // Prevent instantiation
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
