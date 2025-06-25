package com.uni.ea_autoscaler.jmeter;

public interface JMeterExecutor {

    JMeterExecutionResult runTest(String targetHost,
                                  String targetPort,
                                  String testPlanPath,
                                  String resultFilePath,
                                  String logFilePath);
}
