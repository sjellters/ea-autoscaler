package com.uni.ea_autoscaler.k8s;

import com.uni.ea_autoscaler.core.utils.ThreadUtils;
import com.uni.ea_autoscaler.k8s.dto.DeploymentStatusCheck;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class KubernetesDeploymentWaiter {

    private final long readinessPollInterval;
    private final long hashPollInterval;

    private final KubernetesDeploymentInspector inspector;
    private final AppsV1Api appsV1Api;
    private final KubernetesRetryExecutor retryExecutor;

    public KubernetesDeploymentWaiter(@Value("${k8s.deploymentMonitor.readinessPollInterval:5000}") long readinessPollInterval,
                                      @Value("${k8s.deploymentMonitor.hashPollInterval:1000}") long hashPollInterval,
                                      KubernetesDeploymentInspector inspector, AppsV1Api appsV1Api,
                                      KubernetesRetryExecutor retryExecutor) {
        this.readinessPollInterval = readinessPollInterval;
        this.hashPollInterval = hashPollInterval;
        this.inspector = inspector;
        this.appsV1Api = appsV1Api;
        this.retryExecutor = retryExecutor;
    }

    public void waitUntilDeploymentReady(String namespace, String deploymentName, int expectedReplicas, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        int attempt = 1;

        log.info("⏳ Waiting for deployment '{}' in namespace '{}' to become ready (expecting {} pods)...",
                deploymentName, namespace, expectedReplicas);

        while (Instant.now().isBefore(deadline)) {
            DeploymentStatusCheck status = inspector.checkDeploymentState(namespace, deploymentName);

            if (!status.isDeploymentExists()) {
                log.warn("⚠️ Deployment '{}' does not exist yet. Attempt #{}", deploymentName, attempt++);
                dynamicSleepUntil(deadline, readinessPollInterval);
                continue;
            }

            if (!status.getErrorPods().isEmpty()) {
                log.warn("⚠️ Waiting for pods to recover from error state: {}", status.getErrorPods());
                dynamicSleepUntil(deadline, readinessPollInterval);
                continue;
            }

            if (!status.getTerminatingPods().isEmpty()) {
                log.warn("♻️ Waiting for terminating pods to disappear: {}", status.getTerminatingPods());
                dynamicSleepUntil(deadline, readinessPollInterval);
                continue;
            }

            if (status.getReadyPods().size() < expectedReplicas) {
                log.info("⌛ Not enough ready pods ({} of {}). Attempt #{}",
                        status.getReadyPods().size(), expectedReplicas, attempt++);
                dynamicSleepUntil(deadline, readinessPollInterval);
                continue;
            }

            log.info("✅ Deployment '{}' is ready with {} active pods.", deploymentName, expectedReplicas);
            return;
        }

        throw new IllegalStateException("⏰ Timeout while waiting for deployment '" + deploymentName + "' to become ready.");
    }

    public String waitForCurrentPodTemplateHash(String namespace, String deploymentName, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        int attempt = 1;

        log.info("🔍 Waiting for pod-template-hash from latest ReplicaSet of deployment '{}'...", deploymentName);

        while (Instant.now().isBefore(deadline)) {
            DeploymentStatusCheck status = inspector.checkDeploymentState(namespace, deploymentName);
            List<String> allPods = new ArrayList<>();
            allPods.addAll(status.getReadyPods());
            allPods.addAll(status.getErrorPods());
            allPods.addAll(status.getTerminatingPods());

            if (allPods.isEmpty()) {
                log.info("⏳ No pods detected for deployment '{}'. Waiting... (attempt #{})", deploymentName, attempt++);
                dynamicSleepUntil(deadline, readinessPollInterval);
                continue;
            }

            if (!status.getTerminatingPods().isEmpty()) {
                log.info("♻️ Still waiting for terminating pods to disappear: {} (attempt #{})", status.getTerminatingPods(), attempt++);
                dynamicSleepUntil(deadline, readinessPollInterval);
                continue;
            }

            break;
        }

        attempt = 1;
        while (Instant.now().isBefore(deadline)) {
            List<V1ReplicaSet> replicaSets = retryExecutor.executeWithRetry("listReplicaSets:" + deploymentName, () ->
                    appsV1Api.listNamespacedReplicaSet(namespace).execute().getItems()
            );

            Optional<String> latestHash = replicaSets.stream()
                    .filter(rs -> rs.getMetadata() != null &&
                            rs.getMetadata().getOwnerReferences() != null &&
                            rs.getMetadata().getOwnerReferences().stream()
                                    .anyMatch(owner -> "Deployment".equals(owner.getKind()) &&
                                            deploymentName.equals(owner.getName())))
                    .sorted(Comparator.comparing(
                            rs -> Optional.ofNullable(rs.getMetadata().getCreationTimestamp())
                                    .orElse(OffsetDateTime.MIN),
                            Comparator.reverseOrder()
                    ))
                    .map(rs -> {
                        if (rs.getMetadata().getLabels() != null) {
                            return rs.getMetadata().getLabels().get("pod-template-hash");
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst();

            if (latestHash.isPresent()) {
                log.info("✅ Found pod-template-hash '{}' for deployment '{}'", latestHash.get(), deploymentName);
                return latestHash.get();
            }

            log.info("⏳ pod-template-hash not found yet (attempt #{}). Waiting...", attempt++);
            dynamicSleepUntil(deadline, hashPollInterval);
        }

        throw new IllegalStateException("❌ Timeout while waiting for pod-template-hash of deployment '" + deploymentName + "'");
    }

    private void dynamicSleepUntil(Instant deadline, long pollInterval) {
        long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
        long sleepMs = Math.min(pollInterval, Math.max(0, remainingMs));
        if (sleepMs > 0) {
            ThreadUtils.sleep(sleepMs);
        }
    }
}
