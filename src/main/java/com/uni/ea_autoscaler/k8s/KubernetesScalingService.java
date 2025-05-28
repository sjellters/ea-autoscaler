package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KubernetesScalingService {

    private final KubernetesScaler kubernetesScaler;
    private final KubernetesDeploymentMonitor deploymentMonitor;

    public KubernetesScalingService(KubernetesScaler kubernetesScaler,
                                    KubernetesDeploymentMonitor deploymentMonitor) {
        this.kubernetesScaler = kubernetesScaler;
        this.deploymentMonitor = deploymentMonitor;
    }

    public boolean applyAndWait(ScalingConfiguration config) {
        boolean applied = kubernetesScaler.applyScalingConfiguration(config);
        if (!applied) {
            log.warn("❌ Failed to apply scaling configuration.");
            return false;
        }

        log.info("🔄 Waiting for pods to reach desired replica count: {}", config.getMinReplicas());
        try {
            deploymentMonitor.waitForDesiredReplicas(config.getMinReplicas(), 180, 2000);
            Thread.sleep(5000); // Optional stabilization
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🛑 Scaling wait interrupted", e);
            return false;
        }

        log.info("✅ Scaling successfully applied and pods ready.");
        return true;
    }
}
