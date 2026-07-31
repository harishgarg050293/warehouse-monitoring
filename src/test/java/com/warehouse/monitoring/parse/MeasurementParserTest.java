package com.warehouse.monitoring.parse;

import com.warehouse.monitoring.model.SensorMeasurement;
import com.warehouse.monitoring.model.SensorType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementParserTest {

    private final MeasurementParser parser = new MeasurementParser();

    @Test
    void parsesTemperaturePayload() {
        Optional<SensorMeasurement> result =
                parser.parse("sensor_id=t1; value=30", SensorType.TEMPERATURE, "warehouse-1");

        assertThat(result).isPresent();
        assertThat(result.get().sensorId()).isEqualTo("t1");
        assertThat(result.get().value()).isEqualTo(30.0);
        assertThat(result.get().type()).isEqualTo(SensorType.TEMPERATURE);
        assertThat(result.get().warehouseId()).isEqualTo("warehouse-1");
    }

    @Test
    void parsesHumidityPayloadWithSpaces() {
        Optional<SensorMeasurement> result =
                parser.parse("sensor_id = h1 ; value = 40.5", SensorType.HUMIDITY, "warehouse-2");

        assertThat(result).isPresent();
        assertThat(result.get().sensorId()).isEqualTo("h1");
        assertThat(result.get().value()).isEqualTo(40.5);
        assertThat(result.get().type()).isEqualTo(SensorType.HUMIDITY);
    }

    @Test
    void rejectsInvalidPayload() {
        assertThat(parser.parse("bad-payload", SensorType.TEMPERATURE, "warehouse-1")).isEmpty();
        assertThat(parser.parse("", SensorType.TEMPERATURE, "warehouse-1")).isEmpty();
        assertThat(parser.parse(null, SensorType.TEMPERATURE, "warehouse-1")).isEmpty();
    }
}
