package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.common.DeploymentConfig;
import com.uni.ea_autoscaler.common.HPAConfig;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentStrategy;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1RollingUpdateDeployment;
import io.kubernetes.client.openapi.models.V2CrossVersionObjectReference;
import io.kubernetes.client.openapi.models.V2HPAScalingPolicy;
import io.kubernetes.client.openapi.models.V2HPAScalingRules;
import io.kubernetes.client.openapi.models.V2HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V2HorizontalPodAutoscalerBehavior;
import io.kubernetes.client.openapi.models.V2HorizontalPodAutoscalerSpec;
import io.kubernetes.client.openapi.models.V2MetricSpec;
import io.kubernetes.client.openapi.models.V2MetricTarget;
import io.kubernetes.client.openapi.models.V2ResourceMetricSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class KubernetesWorkloadApplier {

    private final AppsV1Api appsV1Api;
    private final AutoscalingV2Api autoscalingV2Api;
    private final KubernetesRetryExecutor retryExecutor;

    public KubernetesWorkloadApplier(AppsV1Api appsV1Api, AutoscalingV2Api autoscalingV2Api, KubernetesRetryExecutor retryExecutor) {
        this.appsV1Api = appsV1Api;
        this.autoscalingV2Api = autoscalingV2Api;
        this.retryExecutor = retryExecutor;
    }

    public void applyDeploymentConfiguration(String namespace, String deploymentName, DeploymentConfig config) {
        retryExecutor.executeWithRetry("applyDeployment", () -> {
            V1Deployment deployment = appsV1Api.readNamespacedDeployment(deploymentName, namespace).execute();

            Optional.ofNullable(deployment.getSpec()).ifPresent(spec -> {
                spec.setReplicas(config.getReplicas());
                spec.setStrategy(buildDeploymentStrategy());

                Optional.of(spec.getTemplate())
                        .map(V1PodTemplateSpec::getSpec)
                        .map(V1PodSpec::getContainers)
                        .filter(c -> !c.isEmpty())
                        .ifPresent(c -> c.get(0).setResources(buildResourceRequirements(config)));

                Optional.of(spec.getTemplate())
                        .map(V1PodTemplateSpec::getMetadata)
                        .ifPresent(meta -> meta.putAnnotationsItem(
                                "kubectl.kubernetes.io/restartedAt",
                                Instant.now().toString()
                        ));
            });

            appsV1Api.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();
            return null;
        });
    }

    public void applyOrReplaceHpa(String namespace, String hpaName, String deploymentName, HPAConfig config) {
        retryExecutor.executeWithRetry("deleteHPA:" + hpaName, () -> {
            try {
                autoscalingV2Api.deleteNamespacedHorizontalPodAutoscaler(hpaName, namespace).execute();
            } catch (ApiException e) {
                if (e.getCode() != 404) throw e;
            }
            return null;
        });

        if (config != null && config.isEnabled()) {
            V2HorizontalPodAutoscaler hpa = new V2HorizontalPodAutoscaler()
                    .apiVersion("autoscaling/v2")
                    .kind("HorizontalPodAutoscaler")
                    .metadata(new V1ObjectMeta().name(hpaName).namespace(namespace))
                    .spec(buildHpaSpec(deploymentName, config));

            retryExecutor.executeWithRetry("applyHPA:" + hpaName, () -> {
                try {
                    autoscalingV2Api.readNamespacedHorizontalPodAutoscaler(hpaName, namespace).execute();
                    autoscalingV2Api.replaceNamespacedHorizontalPodAutoscaler(hpaName, namespace, hpa).execute();
                } catch (ApiException e) {
                    if (e.getCode() == 404) {
                        autoscalingV2Api.createNamespacedHorizontalPodAutoscaler(namespace, hpa).execute();
                    } else {
                        throw e;
                    }
                }
                return null;
            });
        }
    }

    private V1DeploymentStrategy buildDeploymentStrategy() {
        return new V1DeploymentStrategy()
                .type("RollingUpdate")
                .rollingUpdate(new V1RollingUpdateDeployment()
                        .maxSurge(new IntOrString(1))
                        .maxUnavailable(new IntOrString(0)));
    }

    private V1ResourceRequirements buildResourceRequirements(DeploymentConfig config) {
        return new V1ResourceRequirements()
                .putRequestsItem("cpu", Quantity.fromString(config.getCpuRequest() + "m"))
                .putRequestsItem("memory", Quantity.fromString(config.getMemoryRequest() + "Mi"))
                .putLimitsItem("cpu", Quantity.fromString(config.getCpuRequest() + "m"))
                .putLimitsItem("memory", Quantity.fromString(config.getMemoryRequest() + "Mi"));
    }

    private V2HorizontalPodAutoscalerBehavior buildHpaBehavior(HPAConfig config) {
        int cooldown = config.getStabilizationWindowSeconds();
        int periodUp = Math.max(5, cooldown / 2);
        int periodDown = Math.max(10, cooldown);

        return new V2HorizontalPodAutoscalerBehavior()
                .scaleUp(new V2HPAScalingRules()
                        .stabilizationWindowSeconds(cooldown)
                        .policies(List.of(new V2HPAScalingPolicy()
                                .type("Percent")
                                .value(100)
                                .periodSeconds(periodUp))))
                .scaleDown(new V2HPAScalingRules()
                        .stabilizationWindowSeconds(cooldown)
                        .policies(List.of(new V2HPAScalingPolicy()
                                .type("Percent")
                                .value(100)
                                .periodSeconds(periodDown))));
    }

    private V2HorizontalPodAutoscalerSpec buildHpaSpec(String deploymentName, HPAConfig config) {
        return new V2HorizontalPodAutoscalerSpec()
                .minReplicas(config.getMinReplicas())
                .maxReplicas(config.getMaxReplicas())
                .scaleTargetRef(new V2CrossVersionObjectReference()
                        .kind("Deployment")
                        .name(deploymentName)
                        .apiVersion("apps/v1"))
                .metrics(List.of(
                        new V2MetricSpec()
                                .type("Resource")
                                .resource(new V2ResourceMetricSource()
                                        .name("cpu")
                                        .target(new V2MetricTarget()
                                                .type("Utilization")
                                                .averageUtilization((int) (config.getCpuThreshold() * 100)))),
                        new V2MetricSpec()
                                .type("Resource")
                                .resource(new V2ResourceMetricSource()
                                        .name("memory")
                                        .target(new V2MetricTarget()
                                                .type("Utilization")
                                                .averageUtilization((int) (config.getMemoryThreshold() * 100))))
                ))
                .behavior(buildHpaBehavior(config));
    }
}
