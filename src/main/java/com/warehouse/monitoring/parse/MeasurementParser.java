package com.warehouse.monitoring.parse;

import com.warehouse.monitoring.model.SensorMeasurement;
import com.warehouse.monitoring.model.SensorType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses UDP payloads of the form: {@code sensor_id=t1; value=30}
 */
@Component
public class MeasurementParser {

    private static final Pattern PAYLOAD =
            Pattern.compile("(?i)^\\s*sensor_id\\s*=\\s*([^;]+)\\s*;\\s*value\\s*=\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*$");

    public Optional<SensorMeasurement> parse(String raw, SensorType type, String warehouseId) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = PAYLOAD.matcher(raw.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String sensorId = matcher.group(1).trim();
        double value = Double.parseDouble(matcher.group(2));
        return Optional.of(new SensorMeasurement(warehouseId, sensorId, type, value));
    }

    public SensorType inferTypeFromSensorId(String sensorId) {
        String id = sensorId == null ? "" : sensorId.toLowerCase(Locale.ROOT);
        if (id.startsWith("h")) {
            return SensorType.HUMIDITY;
        }
        return SensorType.TEMPERATURE;
    }
}
