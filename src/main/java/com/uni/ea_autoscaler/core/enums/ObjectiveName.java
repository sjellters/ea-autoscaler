package com.uni.ea_autoscaler.core.enums;

public enum ObjectiveName {

    SLA_VIOLATION("slaViolation"),
    CPU_EFFICIENCY_LOSS("cpuEfficiencyLoss"),
//    MEMORY_EFFICIENCY_LOSS("memoryEfficiencyLoss"),
    P95("p95"),
    PROVISIONED_CPU("provisionedCpu"),
    PROVISIONED_MEMORY("provisionedMemory");
//    REPLICA_VARIANCE("replicaVariance");

    private final String value;

    ObjectiveName(String value) {
        this.value = value;
    }

    public static ObjectiveName fromValue(String value) {
        for (ObjectiveName m : values()) {
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
