package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.k8s.dto.DeploymentStatusCheck;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KubernetesDeploymentInspector {

    private final AppsV1Api appsV1Api;
    private final CoreV1Api coreV1Api;
    private final AutoscalingV2Api autoscalingV2Api;
    private final KubernetesRetryExecutor retryExecutor;

    public KubernetesDeploymentInspector(AppsV1Api appsV1Api,
                                         CoreV1Api coreV1Api,
                                         AutoscalingV2Api autoscalingV2Api,
                                         KubernetesRetryExecutor retryExecutor) {
        this.appsV1Api = appsV1Api;
        this.coreV1Api = coreV1Api;
        this.autoscalingV2Api = autoscalingV2Api;
        this.retryExecutor = retryExecutor;
    }

    public DeploymentStatusCheck checkDeploymentState(String namespace, String deploymentName) {
        DeploymentStatusCheck result = new DeploymentStatusCheck();
        result.setDeploymentExists(false);

        V1Deployment deployment;
        try {
            deployment = retryExecutor.executeWithRetry(
                    "readDeployment" + deploymentName,
                    () -> appsV1Api.readNamespacedDeployment(deploymentName, namespace).execute()
            );
            result.setDeploymentExists(true);
        } catch (RuntimeException e) {
            return result;
        }


        Integer replicas = Optional.ofNullable(deployment.getSpec())
                .map(V1DeploymentSpec::getReplicas)
                .orElse(1);
        result.setExpectedReplicas(replicas);

        Map<String, String> labels = Optional.ofNullable(deployment.getSpec())
                .map(V1DeploymentSpec::getSelector)
                .map(V1LabelSelector::getMatchLabels)
                .orElse(Collections.emptyMap());

        String selector = labels.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));

        List<V1Pod> pods = retryExecutor.executeWithRetry(
                "listPodsWithSelector:" + selector,
                () -> coreV1Api.listNamespacedPod(namespace)
                        .labelSelector(selector)
                        .execute()
                        .getItems()
        );

        List<String> readyPods = new ArrayList<>();
        List<String> terminatingPods = new ArrayList<>();
        List<String> errorPods = new ArrayList<>();

        for (V1Pod pod : pods) {
            String name = Optional.ofNullable(pod.getMetadata())
                    .map(V1ObjectMeta::getName)
                    .orElse("UNKNOWN");

            boolean isReady = Optional.ofNullable(pod.getStatus())
                    .map(V1PodStatus::getConditions)
                    .orElse(Collections.emptyList())
                    .stream()
                    .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));

            boolean isTerminating = Optional.ofNullable(pod.getMetadata())
                    .map(V1ObjectMeta::getDeletionTimestamp)
                    .isPresent();

            if (isTerminating) {
                terminatingPods.add(name);
            } else if ("Running".equals(Optional.ofNullable(pod.getStatus())
                    .map(V1PodStatus::getPhase)
                    .orElse("")) && isReady) {
                readyPods.add(name);
            } else {
                errorPods.add(name);
            }
        }

        result.setReadyPods(readyPods);
        result.setTerminatingPods(terminatingPods);
        result.setErrorPods(errorPods);

        boolean hpaExists = retryExecutor.executeWithRetry(
                "checkHPA:" + deploymentName,
                () -> {
                    try {
                        autoscalingV2Api.readNamespacedHorizontalPodAutoscaler(deploymentName, namespace).execute();
                        return true;
                    } catch (io.kubernetes.client.openapi.ApiException e) {
                        if (e.getCode() == 404) return false;
                        throw e;
                    }
                }
        );
        result.setHpaExists(hpaExists);

        return result;
    }
}
