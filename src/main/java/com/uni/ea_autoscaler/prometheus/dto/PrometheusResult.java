package com.uni.ea_autoscaler.prometheus.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PrometheusResult {

    private Map<String, String> metric;
    private List<String> value;
    private List<List<String>> values;
}
