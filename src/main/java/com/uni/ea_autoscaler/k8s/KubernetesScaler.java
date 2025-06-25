package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;

public interface KubernetesScaler {

    boolean applyScalingConfiguration(ScalingConfiguration config);

    void applyStaticDeploymentConfiguration(ScalingConfiguration config);

    void restartDeployment();
}
