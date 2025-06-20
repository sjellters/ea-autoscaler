package com.uni.ea_autoscaler;

import com.uni.ea_autoscaler.ga.evaluation.FitnessEvaluator;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.k8s.KubernetesDeploymentMonitor;
import com.uni.ea_autoscaler.k8s.KubernetesScaler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StaticConfigurationRunner implements CommandLineRunner {

    private final FitnessEvaluator evaluator;
    private final KubernetesScaler scaler;
    private final KubernetesDeploymentMonitor monitor;

    @Autowired
    public StaticConfigurationRunner(FitnessEvaluator evaluator, KubernetesScaler scaler, KubernetesDeploymentMonitor monitor) {
        this.evaluator = evaluator;
        this.scaler = scaler;
        this.monitor = monitor;
    }

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

        scaler.applyStaticDeploymentConfiguration(staticConfig);
        monitor.waitForDesiredReplicas(staticConfig.getMaxReplicas(), 180, 2000);

        String staticId = "static-baseline";
        evaluator.evaluate(staticConfig);

        double[] objectives = staticConfig.getObjectives();
        System.out.println("📊 Static config objectives:");
        System.out.println("  - Avg. Response Time: " + objectives[0]);
        System.out.println("  - Avg. CPU Usage: " + objectives[1]);
        System.out.println("  - Avg. Memory Usage: " + objectives[2]);
        System.out.println("  - Avg. Replicas: " + objectives[3]);
        System.out.println("  - Error Rate: " + objectives[4]);
        System.out.println("  - Avg. Latency: " + objectives[5]);
    }
}
