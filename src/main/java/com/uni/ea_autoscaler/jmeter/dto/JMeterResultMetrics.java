package com.uni.ea_autoscaler.jmeter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JMeterResultMetrics {

    private final double averageResponseTime;
    private final double averageLatency;
    private final double errorRate;
}
