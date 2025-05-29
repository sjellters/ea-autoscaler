package com.uni.ea_autoscaler.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KubernetesDeploymentMonitor {

    private final CoreV1Api coreV1Api;

    @Value("${monitor.namespace}")
    private String namespace;

    @Value("${monitor.labelSelector}")
    private String labelSelector;

    public KubernetesDeploymentMonitor(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    public void waitForDesiredReplicas(int desiredReplicas, int maxAttempts, int pollIntervalMillis) throws InterruptedException {
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                V1PodList podList = coreV1Api
                        .listNamespacedPod(namespace)
                        .labelSelector(labelSelector)
                        .execute();

                List<V1Pod> items = podList.getItems();
                long readyPods = items.stream()
                        .filter(pod -> pod.getStatus() != null &&
                                pod.getStatus().getConditions() != null &&
                                pod.getStatus().getConditions().stream()
                                        .anyMatch(cond -> "Ready".equals(cond.getType()) && "True".equals(cond.getStatus()))
                        ).count();

                log.info("🔍 Ready pods: {}/{}", readyPods, desiredReplicas);

                if (readyPods == desiredReplicas) {
                    return;
                }

                Thread.sleep(pollIntervalMillis);
                attempt++;
            } catch (ApiException e) {
                log.error("❌ Error fetching pod list from namespace '{}':", namespace, e);
                throw new RuntimeException(e);
            }
        }
        log.warn("⚠️ Timed out waiting for desired replicas ({}). Proceeding anyway.", desiredReplicas);
    }
}
