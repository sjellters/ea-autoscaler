package com.uni.ea_autoscaler.ga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ScalingConfigurationDeserializationTest {

    @Test
    public void testDeserializationFromJson() throws IOException {
        String json = """
                {
                  "minReplicas":1,
                  "maxReplicas":3,
                  "cpuThreshold":0.3,
                  "memoryThreshold":0.6,
                  "cooldownSeconds":15,
                  "cpuRequest":150,
                  "memoryRequest":768,
                  "metrics":{
                    "avgResponseTime":9755.175,
                    "avgCpu":0.29370341320916277,
                    "avgMemory":2.210649351278941E-5,
                    "avgReplicas":2.8,
                    "errorRate":0.9960374461340333,
                    "avgLatency":9755.05,
                    "cpuEfficiency":0.6992938409741971,
                    "memoryEfficiency":1.0280177414801625E-8,
                    "slaPercentage":0.325,
                    "p95":21521.0
                  },
                  "objectivesMap":{
                    "slaViolation":0.675,
                    "avgCpu":0.29370341320916277,
                    "avgMemory":2.210649351278941E-5,
                    "avgReplicas":2.8,
                    "cpuEfficiencyLoss":1.4300140247294106,
                    "memoryEfficiencyLoss":9.727458580239852E7,
                    "errorRate":0.9960374461340333
                  },
                  "penalizedObjectivesMap":{
                    "slaViolation":34.425,
                    "avgCpu":0.29370341320916277,
                    "avgMemory":2.210649351278941E-5,
                    "avgReplicas":2.8,
                    "cpuEfficiencyLoss":1.4300140247294106,
                    "memoryEfficiencyLoss":9.727460580239852E7,
                    "errorRate":50.7979097528357
                  },
                  "objectives":[34.425,0.29370341320916277,2.210649351278941E-5,2.8,1.4300140247294106,9.727460580239852E7,50.7979097528357],
                  "normalizedObjectives":null
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        ScalingConfiguration config = mapper.readValue(json, ScalingConfiguration.class);

        assertNotNull(config);
        assertEquals(1, config.getMinReplicas());
        assertEquals(3, config.getMaxReplicas());
        assertNotNull(config.getObjectives());
        assertEquals(7, config.getObjectives().length);
        assertNotNull(config.getPenalizedObjectivesMap());
        assertEquals(7, config.getPenalizedObjectivesMap().size());
    }
}
