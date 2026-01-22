package com.rosanova.iot.timer.synchronization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class MultithreadSynchronization {

    @Bean
    ReentrantLock getIOMutex(){
        return new ReentrantLock(true);
    }

    @Bean
    ExecutorService getPoolThread(){
        return Executors.newFixedThreadPool(3);
    }
}
