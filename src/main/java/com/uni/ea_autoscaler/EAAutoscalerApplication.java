package com.uni.ea_autoscaler;

import com.uni.ea_autoscaler.baseline.BaselineConfigurationRunner;
import com.uni.ea_autoscaler.ga.nsga3.EvolutionRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication
public class EAAutoscalerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EAAutoscalerApplication.class, args);

        try (context) {
            BaselineConfigurationRunner baselineRunner = context.getBean(BaselineConfigurationRunner.class);
            EvolutionRunner evolutionRunner = context.getBean(EvolutionRunner.class);
            log.info("🚀 Running Baseline evaluation...");
            baselineRunner.run();

            log.info("🧬 Running Evolutionary algorithm...");
            evolutionRunner.run();
        } catch (Exception e) {
            log.error("❌ Error during evaluation: {}", e.getMessage(), e);
        }
    }
}

