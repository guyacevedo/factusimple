package com.guyacevedo.factusimple.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (ExpiredAuthDataCleanup). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}