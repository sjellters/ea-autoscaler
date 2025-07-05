package com.uni.ea_autoscaler.jmeter.dto;

public record JTLSample(
        Long elapsed,
        Long latency,
        Long connect,
        Long idleTime,
        Boolean success
) {
    public boolean hasNullFields() {
        return elapsed == null || latency == null || connect == null || idleTime == null || success == null;
    }
}
