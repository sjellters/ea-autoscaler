package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

public interface KubernetesScaler {

    boolean applyScalingConfiguration(ScalingConfiguration config);
}
