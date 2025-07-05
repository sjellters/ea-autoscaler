package com.uni.ea_autoscaler.core.annotations;

import com.uni.ea_autoscaler.core.enums.MetricName;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ValidatesMetrics {

    MetricName[] value();
}
