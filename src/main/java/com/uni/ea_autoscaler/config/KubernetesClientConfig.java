package com.uni.ea_autoscaler.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class KubernetesClientConfig {

    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client = Config.defaultClient();
        client.setVerifyingSsl(false);
        client.setConnectTimeout(5000);
        client.setReadTimeout(10000);
        client.setWriteTimeout(10000);

        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

        return client;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient client) {
        return new CoreV1Api(client);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient client) {
        return new AppsV1Api(client);
    }

    @Bean
    public AutoscalingV2Api autoscalingV2Api(ApiClient client) {
        return new AutoscalingV2Api(client);
    }
}
