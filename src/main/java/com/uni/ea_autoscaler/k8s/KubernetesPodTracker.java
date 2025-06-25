package com.uni.ea_autoscaler.k8s;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class KubernetesPodTracker {

    private final CoreV1Api coreV1Api;
    private final Set<String> trackedPods = new HashSet<>();

    @Setter
    private String currentPodTemplateHash = "";

    @Value("${monitor.namespace}")
    private String namespace;

    @Value("${monitor.labelSelector}")
    private String labelSelector;

    public void reset() {
        trackedPods.clear();
    }

    public Set<String> getTrackedPods() {
        return new HashSet<>(trackedPods);
    }

    public void pollPods() {
        try {
            V1PodList podList = coreV1Api
                    .listNamespacedPod(namespace)
                    .labelSelector(labelSelector)
                    .execute();

            for (V1Pod pod : podList.getItems()) {
                V1ObjectMeta metadata = pod.getMetadata();
                if (metadata == null || pod.getStatus() == null) continue;

                String phase = pod.getStatus().getPhase();
                if (!"Running".equals(phase)) continue;

                Map<String, String> labels = metadata.getLabels();
                if (labels == null) continue;
                String hash = labels.get("pod-template-hash");
                if (hash == null || !hash.equals(currentPodTemplateHash)) continue;

                String name = metadata.getName();
                if (trackedPods.add(name)) {
                    log.info("🆕 New running pod discovered: {}", name);
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to poll pods: {}", e.getMessage(), e);
        }
    }
}
