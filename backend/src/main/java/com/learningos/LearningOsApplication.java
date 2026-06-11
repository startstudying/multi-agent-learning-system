package com.learningos;

import com.learningos.config.AiModelProperties;
import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import com.learningos.config.IndexRecoveryProperties;
import com.learningos.config.IndexWorkerProperties;
import com.learningos.config.ModelProviderProperties;
import com.learningos.config.OpsAlertProperties;
import com.learningos.config.RagVectorProperties;
import com.learningos.config.TokenBudgetProperties;
import com.learningos.config.RagProperties;
import com.learningos.config.RagParserOcrProperties;
import com.learningos.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        AppProperties.class,
        AuthProperties.class,
        StorageProperties.class,
        RagProperties.class,
        RagParserOcrProperties.class,
        AiModelProperties.class,
        ModelProviderProperties.class,
        TokenBudgetProperties.class,
        OpsAlertProperties.class,
        com.learningos.config.RagVectorProperties.class,
        IndexRecoveryProperties.class,
        IndexWorkerProperties.class
})
public class LearningOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningOsApplication.class, args);
    }
}
