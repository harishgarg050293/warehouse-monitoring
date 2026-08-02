package com.warehouse.monitoring.parse;

import com.warehouse.monitoring.model.SensorMeasurement;
import com.warehouse.monitoring.model.SensorType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MeasurementParser {

    public Optional<SensorMeasurement> parse(String raw, SensorType type, String warehouseId) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String[] parts = raw.trim().split(";");
        if (parts.length != 2) {
            return Optional.empty();
        }

        String sensorId = readField(parts[0], "sensor_id");
        String valueText = readField(parts[1], "value");
        if (sensorId == null || valueText == null) {
            return Optional.empty();
        }

        try {
            double value = Double.parseDouble(valueText);
            return Optional.of(new SensorMeasurement(warehouseId, sensorId, type, value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String readField(String part, String key) {
        String[] tokens = part.trim().split("=", 2);
        if (tokens.length != 2 || !tokens[0].trim().equalsIgnoreCase(key)) {
            return null;
        }
        return tokens[1].trim();
    }
}
