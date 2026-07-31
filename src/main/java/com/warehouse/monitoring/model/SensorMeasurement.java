package com.warehouse.monitoring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SensorMeasurement(
        String warehouseId,
        String sensorId,
        SensorType type,
        double value
) {
}
