package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class KubernetesWorkloadOrchestrator {

    private final KubernetesWorkloadApplier workloadApplier;
    private final KubernetesDeploymentWaiter deploymentWaiter;

    @Getter
    private final KubernetesPodTracker podTracker;

    private final String namespace;
    private final String deploymentName;
    private final String hpaName;
    private final Duration waitTimeout;

    public KubernetesWorkloadOrchestrator(KubernetesWorkloadApplier workloadApplier,
                                          KubernetesDeploymentWaiter deploymentWaiter,
                                          KubernetesPodTracker podTracker,
                                          @Value("${k8s.namespace}") String namespace,
                                          @Value("${k8s.deploymentName}") String deploymentName,
                                          @Value("${k8s.hpaName}") String hpaName,
                                          @Value("${k8s.waitTimeoutSeconds:30}") long waitTimeoutSeconds) {
        this.workloadApplier = workloadApplier;
        this.deploymentWaiter = deploymentWaiter;
        this.podTracker = podTracker;
        this.namespace = namespace;
        this.deploymentName = deploymentName;
        this.hpaName = hpaName;
        this.waitTimeout = Duration.ofSeconds(waitTimeoutSeconds);
    }

    public void prepareEnvironment(ResourceScalingConfig config) {
        log.info("🚀 Applying deployment configuration for '{}'", deploymentName);
        workloadApplier.applyDeploymentConfiguration(namespace, deploymentName, config.getDeployment());

        if (config.getHpa() != null) {
            log.info("⚙️  Applying HPA configuration for '{}'", deploymentName);
            workloadApplier.applyOrReplaceHpa(namespace, hpaName, deploymentName, config.getHpa());
        }

        String podTemplateHash = deploymentWaiter.waitForCurrentPodTemplateHash(namespace, deploymentName, waitTimeout);
        podTracker.setCurrentPodTemplateHash(podTemplateHash);

        deploymentWaiter.waitUntilDeploymentReady(namespace, deploymentName, config.getDeployment().getReplicas(), waitTimeout);
        podTracker.recordCurrentPods(namespace);
    }
}
