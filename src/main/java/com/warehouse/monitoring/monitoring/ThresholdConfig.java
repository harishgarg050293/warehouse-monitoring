package com.warehouse.monitoring.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring.thresholds")
public record ThresholdConfig(double temperature, double humidity) {
}
