package com.uni.ea_autoscaler.prometheus.dto;

import lombok.Data;

import java.util.List;

@Data
public class PrometheusData {

    private String resultType;
    private List<PrometheusResult> result;
}
