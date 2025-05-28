package com.uni.ea_autoscaler.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class KubernetesDeploymentMonitor {

    private final AppsV1Api appsApi;
    private final String deploymentName;
    private final String namespace;

    public KubernetesDeploymentMonitor(
            AppsV1Api appsApi,
            @Value("${k8s.deploymentName}") String deploymentName,
            @Value("${k8s.namespace}") String namespace
    ) {
        this.appsApi = appsApi;
        this.deploymentName = deploymentName;
        this.namespace = namespace;
    }

    @SuppressWarnings("BusyWait")
    public void waitForDesiredReplicas(int desiredReplicas, int timeoutSeconds, int pollIntervalMillis) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutSeconds * 2000L) {
            try {
                V1Deployment deployment = appsApi.readNamespacedDeployment(deploymentName, namespace).execute();

                int available = Objects.requireNonNull(deployment.getStatus()).getAvailableReplicas() != null
                        ? deployment.getStatus().getAvailableReplicas() : 0;

                log.info("🔍 Waiting for {} available replicas... Current: {}", desiredReplicas, available);

                if (available == desiredReplicas) {
                    log.info("✅ Desired number of replicas reached.");
                    return;
                }

            } catch (ApiException e) {
                log.warn("⚠️ Error while checking deployment status: {}", e.getMessage());
            }

            Thread.sleep(pollIntervalMillis);
        }

        log.warn("⏱️ Timeout while waiting for {} available replicas.", desiredReplicas);
    }
}
