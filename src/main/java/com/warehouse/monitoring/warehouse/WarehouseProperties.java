package com.warehouse.monitoring.warehouse;

import com.warehouse.monitoring.model.SensorType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "warehouse")
public record WarehouseProperties(String id, List<SensorConfig> sensors) {

    public record SensorConfig(SensorType type, int port) {
    }
}
