package com.uni.ea_autoscaler.baseline;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.k8s.KubernetesDeploymentMonitor;
import com.uni.ea_autoscaler.k8s.KubernetesScaler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineConfigurationRunner {

    private final BaselineEvaluator evaluator;
    private final KubernetesScaler scaler;
    private final KubernetesDeploymentMonitor monitor;

    public void run() throws Exception {
        ScalingConfiguration staticConfig = new ScalingConfiguration();
        staticConfig.setMinReplicas(3);
        staticConfig.setMaxReplicas(3);
        staticConfig.setCpuRequest(112);
        staticConfig.setMemoryRequest(576);
        staticConfig.setCpuThreshold(0.8);
        staticConfig.setMemoryThreshold(0.8);
        staticConfig.setCooldownSeconds(30);

        scaler.restartDeployment();
        scaler.applyStaticDeploymentConfiguration(staticConfig);
        monitor.waitForDesiredReplicas(staticConfig.getMaxReplicas(), 180, 2000);
        evaluator.evaluate(staticConfig);
    }
}
