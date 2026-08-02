package com.warehouse.monitoring.warehouse;

import com.warehouse.monitoring.model.SensorType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "")
public record WarehousesProperties(List<WarehouseConfig> warehouses) {

    public record WarehouseConfig(String id, List<SensorConfig> sensors) {
    }

    public record SensorConfig(SensorType type, int port) {
    }
}
