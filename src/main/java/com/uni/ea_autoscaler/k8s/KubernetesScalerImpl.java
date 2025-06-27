package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V2CrossVersionObjectReference;
import io.kubernetes.client.openapi.models.V2HPAScalingPolicy;
import io.kubernetes.client.openapi.models.V2HPAScalingRules;
import io.kubernetes.client.openapi.models.V2HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V2HorizontalPodAutoscalerBehavior;
import io.kubernetes.client.openapi.models.V2HorizontalPodAutoscalerSpec;
import io.kubernetes.client.openapi.models.V2MetricSpec;
import io.kubernetes.client.openapi.models.V2MetricTarget;
import io.kubernetes.client.openapi.models.V2ResourceMetricSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KubernetesScalerImpl implements KubernetesScaler {

    private final AppsV1Api appsApi;
    private final AutoscalingV2Api autoscalingApi;
    private final KubernetesPodTracker kubernetesPodTracker;

    private final String deploymentName;
    private final String hpaName;
    private final String namespace;

    public KubernetesScalerImpl(
            ApiClient apiClient, KubernetesPodTracker kubernetesPodTracker,
            @Value("${k8s.deploymentName}") String deploymentName,
            @Value("${k8s.hpaName}") String hpaName,
            @Value("${k8s.namespace}") String namespace
    ) {
        this.appsApi = new AppsV1Api(apiClient);
        this.autoscalingApi = new AutoscalingV2Api(apiClient);
        this.kubernetesPodTracker = kubernetesPodTracker;
        this.deploymentName = deploymentName;
        this.hpaName = hpaName;
        this.namespace = namespace;
    }

    public void applyStaticDeploymentConfiguration(ScalingConfiguration config) {
        updateDeploymentOnly(config, false);
    }

    @Override
    public void restartDeployment() {
        deleteHpaIfExists();
        try {
            V1Deployment deployment = appsApi.readNamespacedDeployment(deploymentName, namespace).execute();

            V1DeploymentSpec spec = deployment.getSpec();
            if (spec == null) return;

            V1PodTemplateSpec template = spec.getTemplate();

            V1ObjectMeta metadata = template.getMetadata();
            if (metadata == null) return;

            Map<String, String> annotations = metadata.getAnnotations();
            if (annotations == null) annotations = new HashMap<>();

            annotations.put("kubectl.kubernetes.io/restartedAt", Instant.now().toString());
            metadata.setAnnotations(annotations);

            appsApi.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();
            log.info("♻️ Deployment {} restarted", deploymentName);

            // Sleep to allow the deployment to stabilize
            Thread.sleep(5000);
        } catch (Exception e) {
            log.error("❌ Failed to restart deployment: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean applyScalingConfiguration(ScalingConfiguration config) {
        try {
            deleteHpaIfExists();

            boolean updated = updateDeploymentOnly(config, true);
            if (!updated) return false;

            V2HorizontalPodAutoscaler hpa = buildHpaFromConfig(config);
            autoscalingApi.createNamespacedHorizontalPodAutoscaler(namespace, hpa).execute();
            log.info("🔁 HPA created successfully.");

            return true;
        } catch (Exception e) {
            log.error("❌ Error applying scaling configuration: {}", e.getMessage(), e);

            return false;
        }
    }

    private boolean updateDeploymentOnly(ScalingConfiguration config, boolean logPurpose) {
        try {
            V1Deployment deployment = appsApi.readNamespacedDeployment(deploymentName, namespace).execute();
            updateDeploymentResources(deployment, config);
            appsApi.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();

            if (logPurpose) {
                log.info("✅ Deployment updated successfully.");
            } else {
                log.info("✅ Deployment updated successfully (static config, no HPA).");
            }

            Thread.sleep(1000);

            String appLabel = null;
            if (deployment.getSpec() != null
                    && deployment.getSpec().getTemplate().getMetadata() != null
                    && deployment.getSpec().getTemplate().getMetadata().getLabels() != null) {
                appLabel = deployment.getSpec().getTemplate().getMetadata().getLabels().get("app");
            }

            if (appLabel == null) {
                log.warn("⚠️ Cannot determine app label to query ReplicaSets.");
                return true;
            }

            List<V1ReplicaSet> replicaSets = appsApi.listNamespacedReplicaSet(namespace).execute().getItems();

            replicaSets.stream()
                    .filter(rs -> {
                        if (rs.getMetadata() == null) return false;

                        List<V1OwnerReference> refs = rs.getMetadata().getOwnerReferences();

                        if (refs == null) return false;

                        return refs.stream().anyMatch(r ->
                                "Deployment".equals(r.getKind()) && deploymentName.equals(r.getName()));
                    })
                    .max((a, b) -> {
                        OffsetDateTime t1 = a.getMetadata() != null ? a.getMetadata().getCreationTimestamp() : null;
                        OffsetDateTime t2 = b.getMetadata() != null ? b.getMetadata().getCreationTimestamp() : null;

                        if (t1 == null || t2 == null) return 0;

                        return t1.compareTo(t2);
                    })
                    .ifPresentOrElse(rs -> {
                        if (rs.getMetadata() != null && rs.getMetadata().getLabels() != null) {
                            String hash = rs.getMetadata().getLabels().get("pod-template-hash");

                            if (hash != null) {
                                kubernetesPodTracker.setCurrentPodTemplateHash(hash);
                                log.info("🔑 pod-template-hash set from ReplicaSet: {}", hash);
                            } else {
                                log.warn("⚠️ ReplicaSet found but no pod-template-hash label.");
                            }
                        } else {
                            log.warn("⚠️ ReplicaSet metadata or labels are null.");
                        }
                    }, () -> log.warn("⚠️ No ReplicaSet found for deployment '{}' to extract pod-template-hash.", deploymentName));

            return true;

        } catch (ApiException e) {
            if (e.getCode() == 409) {
                try {
                    Thread.sleep(500);

                    return updateDeploymentOnly(config, logPurpose);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } else if (e.getCode() == 404) {
                log.error("❌ Deployment '{}' not found in namespace '{}'", deploymentName, namespace);
            } else {
                log.error("❌ Failed to read Deployment: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("❌ Error updating deployment: {}", e.getMessage(), e);
        }

        return false;
    }

    private void deleteHpaIfExists() {
        try {
            autoscalingApi.deleteNamespacedHorizontalPodAutoscaler(hpaName, namespace).execute();
            log.info("🗑️  Existing HPA deleted.");
        } catch (Exception e) {
            log.warn("⚠️  Could not delete HPA (might not exist): {}", e.getMessage());
        }
    }

    private void updateDeploymentResources(V1Deployment deployment, ScalingConfiguration config) {
        if (deployment.getSpec() == null ||
                deployment.getSpec().getTemplate().getSpec() == null ||
                deployment.getSpec().getTemplate().getSpec().getContainers().isEmpty()) {
            throw new IllegalStateException("Deployment structure is malformed.");
        }

        deployment.getSpec().setReplicas(config.getMinReplicas());

        V1Container container = deployment.getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0);

        container.setResources(new V1ResourceRequirements()
                .requests(Map.of(
                        "cpu", Quantity.fromString(config.getCpuRequest() + "m"),
                        "memory", Quantity.fromString(config.getMemoryRequest() + "Mi")
                ))
                .limits(Map.of(
                        "cpu", Quantity.fromString((config.getCpuRequest() * 2) + "m"),
                        "memory", Quantity.fromString((int) (config.getMemoryRequest() * 1.5) + "Mi")
                ))
        );
    }

    private V2HorizontalPodAutoscaler buildHpaFromConfig(ScalingConfiguration config) {
        var behavior = new V2HorizontalPodAutoscalerBehavior()
                .scaleUp(new V2HPAScalingRules()
                        .stabilizationWindowSeconds(config.getCooldownSeconds())
                        .policies(List.of(
                                new V2HPAScalingPolicy()
                                        .type("Percent")
                                        .value(100)
                                        .periodSeconds(15)
                        )))
                .scaleDown(new V2HPAScalingRules()
                        .stabilizationWindowSeconds(config.getCooldownSeconds())
                        .policies(List.of(
                                new V2HPAScalingPolicy()
                                        .type("Percent")
                                        .value(100)
                                        .periodSeconds(30)
                        )));

        return new V2HorizontalPodAutoscaler()
                .metadata(new V1ObjectMeta()
                        .name(hpaName)
                        .namespace(namespace))
                .spec(new V2HorizontalPodAutoscalerSpec()
                        .scaleTargetRef(new V2CrossVersionObjectReference()
                                .apiVersion("apps/v1")
                                .kind("Deployment")
                                .name(deploymentName))
                        .minReplicas(config.getMinReplicas())
                        .maxReplicas(config.getMaxReplicas())
                        .metrics(List.of(
                                new V2MetricSpec().type("Resource").resource(
                                        new V2ResourceMetricSource()
                                                .name("cpu")
                                                .target(new V2MetricTarget()
                                                        .type("Utilization")
                                                        .averageUtilization((int) (config.getCpuThreshold() * 100)))
                                ),
                                new V2MetricSpec().type("Resource").resource(
                                        new V2ResourceMetricSource()
                                                .name("memory")
                                                .target(new V2MetricTarget()
                                                        .type("Utilization")
                                                        .averageUtilization((int) (config.getMemoryThreshold() * 100)))
                                )
                        ))
                        .behavior(behavior)
                );
    }
}
