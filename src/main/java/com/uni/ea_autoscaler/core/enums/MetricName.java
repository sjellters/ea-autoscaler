package com.uni.ea_autoscaler.core.enums;

public enum MetricName {

    AVG_RESPONSE_TIME("avgResponseTime"),
    AVG_LATENCY("avgLatency"),
    ERROR_RATE("errorRate"),
    SLA_PERCENTAGE("slaPercentage"),
    P25("p25"),
    P50("p50"),
    P95("p95"),
    AVG_CPU("avgCpu"),
    AVG_MEMORY("avgMemory"),
    AVG_REPLICAS("avgReplicas"),
    CPU_EFFICIENCY("cpuEfficiency"),
    MEMORY_EFFICIENCY("memoryEfficiency"),
    REPLICA_VARIANCE("replicaVariance");

    private final String value;

    MetricName(String value) {
        this.value = value;
    }

    public static MetricName fromValue(String value) {
        for (MetricName m : values()) {
            if (m.value().equals(value)) {
                return m;
            }
        }
        return null;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
