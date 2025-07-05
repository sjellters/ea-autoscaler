package com.uni.ea_autoscaler.prometheus;

import com.uni.ea_autoscaler.prometheus.dto.PrometheusQueryResult;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PrometheusResultProcessor {

    public PrometheusQueryResult process(PrometheusResponse response) {
        if (response == null || !response.isValid()) {
            log.warn("⚠️ Prometheus response is invalid or contains no data");
            return new PrometheusQueryResult(List.of(), false);
        }

        return new PrometheusQueryResult(response.data().result(), true);
    }
}

