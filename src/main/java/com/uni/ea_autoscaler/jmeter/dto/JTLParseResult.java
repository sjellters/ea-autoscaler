package com.uni.ea_autoscaler.jmeter.dto;

import com.uni.ea_autoscaler.core.interfaces.Validatable;

import java.util.List;

public record JTLParseResult(
        List<JTLSample> samples,
        boolean valid
) implements Validatable {
}
