package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesScalingService {

    private final KubernetesScaler kubernetesScaler;
    private final KubernetesDeploymentMonitor deploymentMonitor;

    public boolean applyAndWait(ScalingConfiguration config) {
        boolean applied = kubernetesScaler.applyScalingConfiguration(config);

        if (!applied) {
            log.warn("❌ Failed to apply scaling configuration.");
            return false;
        }

        try {
            log.info("📥 Scaling configuration applied successfully. Monitoring deployment to reach {} replicas...", config.getMinReplicas());

            deploymentMonitor.waitForDesiredReplicas(config.getMinReplicas(), 180, 2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🛑 Scaling wait interrupted", e);

            return false;
        }

        log.info("✅ Scaling successfully applied and pods ready.");

        return true;
    }
}
