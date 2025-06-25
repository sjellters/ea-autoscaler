package com.uni.ea_autoscaler.old;

import com.uni.ea_autoscaler.k8s.KubernetesDeploymentMonitor;
import com.uni.ea_autoscaler.k8s.KubernetesScaler;
import com.uni.ea_autoscaler.old.ga.evaluation.FitnessEvaluator;
import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaticConfigurationRunner implements CommandLineRunner {

    private final FitnessEvaluator evaluator;
    private final KubernetesScaler scaler;
    private final KubernetesDeploymentMonitor monitor;

    @Override
    public void run(String... args) throws Exception {
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

        double[] objectives = staticConfig.getObjectives();
        log.info("📊 Static config objectives:");
        log.info("  - Avg. Response Time: {}", objectives[0]);
        log.info("  - Avg. CPU Usage: {}", objectives[1]);
        log.info("  - Avg. Memory Usage: {}", objectives[2]);
        log.info("  - Avg. Replicas: {}", objectives[3]);
        log.info("  - Error Rate: {}", objectives[4]);
        log.info("  - Avg. Latency: {}", objectives[5]);
    }
}
