package com.uni.ea_autoscaler.common;

public record BaselineValues(
        double slaThreshold
) {
    public boolean valid() {
        return slaThreshold >= 0;
    }
}
