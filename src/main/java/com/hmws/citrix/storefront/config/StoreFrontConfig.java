package com.hmws.citrix.storefront.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@Slf4j
public class StoreFrontConfig {

    @Value("${citrix.storefront.server.base-url}")
    private String storeFrontBaseUrl;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("storefront-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setErrorHandler(throwable ->
                log.error("Citrix scheduled task error occurred", throwable));

        return scheduler;
    }

    @Bean
    public String storeFrontBaseUrl() {
        return storeFrontBaseUrl;
    }
}
