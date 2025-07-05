package com.uni.ea_autoscaler.common;

import com.uni.ea_autoscaler.core.interfaces.Validatable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeploymentConfig implements Validatable {

    private int replicas;
    private int cpuRequest;
    private int memoryRequest;

    public boolean valid() {
        return replicas >= 1 &&
                cpuRequest > 0 &&
                memoryRequest > 0;
    }

    @Override
    public String toString() {
        return String.format("%d-%d-%d", replicas, cpuRequest, memoryRequest);
    }
}
