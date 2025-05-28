package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KubernetesScalerImpl implements KubernetesScaler {

    private final AppsV1Api appsApi;
    private final AutoscalingV2Api autoscalingApi;

    private final String deploymentName;
    private final String hpaName;
    private final String namespace;

    public KubernetesScalerImpl(
            ApiClient apiClient,
            @Value("${k8s.deploymentName}") String deploymentName,
            @Value("${k8s.hpaName}") String hpaName,
            @Value("${k8s.namespace}") String namespace
    ) {
        this.appsApi = new AppsV1Api(apiClient);
        this.autoscalingApi = new AutoscalingV2Api(apiClient);
        this.deploymentName = deploymentName;
        this.hpaName = hpaName;
        this.namespace = namespace;
    }

    @Override
    public boolean applyScalingConfiguration(ScalingConfiguration config) {
        try {
            deleteHpaIfExists();

            V1Deployment deployment;
            try {
                deployment = appsApi.readNamespacedDeployment(deploymentName, namespace).execute();
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    log.error("❌ Deployment '{}' not found in namespace '{}'", deploymentName, namespace);
                } else {
                    log.error("❌ Failed to read Deployment: {}", e.getMessage(), e);
                }
                return false;
            }

            updateDeploymentResources(deployment, config);
            appsApi.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();
            log.info("✅ Deployment updated successfully.");

            V2HorizontalPodAutoscaler hpa = buildHpaFromConfig(config);
            autoscalingApi.createNamespacedHorizontalPodAutoscaler(namespace, hpa).execute();
            log.info("🔁 HPA created successfully.");

            return true;

        } catch (Exception e) {
            log.error("❌ Error applying scaling configuration: {}", e.getMessage(), e);
            return false;
        }
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
                );
    }
}
