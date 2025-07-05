package com.uni.ea_autoscaler.k8s;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KubernetesPodTracker {

    private final CoreV1Api coreV1Api;
    private final KubernetesRetryExecutor retryExecutor;

    private final Set<String> trackedPods = ConcurrentHashMap.newKeySet();
    private String currentPodTemplateHash;

    public KubernetesPodTracker(CoreV1Api coreV1Api, KubernetesRetryExecutor retryExecutor) {
        this.coreV1Api = coreV1Api;
        this.retryExecutor = retryExecutor;
    }

    public void setCurrentPodTemplateHash(String hash) {
        this.currentPodTemplateHash = hash;
        this.trackedPods.clear();
    }

    public void recordCurrentPods(String namespace) {
        if (currentPodTemplateHash == null) {
            throw new IllegalStateException("Pod-template-hash not set");
        }

        var pods = retryExecutor.executeWithRetry("recordPods:" + currentPodTemplateHash, () ->
                coreV1Api.listNamespacedPod(namespace)
                        .labelSelector("pod-template-hash=" + currentPodTemplateHash)
                        .execute()
                        .getItems()
        );

        for (var pod : pods) {
            Optional.ofNullable(pod.getMetadata()).map(V1ObjectMeta::getName).ifPresent(trackedPods::add);
        }
    }

    public Set<String> getTrackedPods() {
        return Set.copyOf(trackedPods);
    }
}

