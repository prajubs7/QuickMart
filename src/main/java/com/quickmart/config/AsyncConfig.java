package com.quickmart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

// Turns on @Async. Without it, @Async is silently ignored and everything runs on the caller's thread.
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * A dedicated pool for emails, so slow SMTP can't take threads from other async work.
     * How the pool grows: first corePoolSize threads, then the QUEUE fills up,
     * and only when the queue is full does it add threads up to maxPoolSize.
     */
    @Bean(name = "emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        // Bounded on purpose. The default is Integer.MAX_VALUE: under load the queue grows
        // silently until OutOfMemoryError, and maxPoolSize is never reached.
        executor.setQueueCapacity(100);
        // Threads named email-1, email-2... so the logs show WHERE code ran.
        executor.setThreadNamePrefix("email-");
        // Pool and queue both full: the caller (the HTTP thread) sends the email itself.
        // That request gets slower, but no email is dropped. The slowdown is natural back-pressure.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // On shutdown, finish queued emails (up to 20 s) instead of dropping them.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
