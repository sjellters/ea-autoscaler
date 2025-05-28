package com.uni.ea_autoscaler.jmeter;

import java.nio.file.Path;

public record JMeterExecutionResult(

        boolean success,
        Path resultsFile
) {
}

