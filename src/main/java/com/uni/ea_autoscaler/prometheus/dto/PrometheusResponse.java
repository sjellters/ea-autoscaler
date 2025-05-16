package com.uni.ea_autoscaler.prometheus.dto;

import lombok.Data;

@Data
public class PrometheusResponse {

    private String status;
    private PrometheusData data;
}
