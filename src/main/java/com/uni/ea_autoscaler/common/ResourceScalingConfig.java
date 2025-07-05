package com.uni.ea_autoscaler.common;

import com.uni.ea_autoscaler.core.interfaces.Validatable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResourceScalingConfig implements Validatable {

    private DeploymentConfig deployment;
    private HPAConfig hpa;

    public boolean valid() {
        return deployment.valid() && hpa.valid();
    }

    @Override
    public String toString() {
        return String.format("d:%s,h:%s", deployment.toString(), hpa.toString());
    }
}
