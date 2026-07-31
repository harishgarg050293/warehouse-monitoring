package com.warehouse.monitoring.monitoring;

import com.warehouse.monitoring.model.SensorMeasurement;
import com.warehouse.monitoring.model.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CentralMonitoringServiceTest {

    private CentralMonitoringService service;

    @BeforeEach
    void setUp() {
        service = new CentralMonitoringService(new ThresholdConfig(35, 50));
    }

    @Test
    void raisesAlarmWhenTemperatureExceedsThreshold(CapturedOutput output) {
        service.evaluate(new SensorMeasurement("warehouse-1", "t1", SensorType.TEMPERATURE, 36));

        assertThat(output.getOut()).contains("ALARM");
        assertThat(output.getOut()).contains("t1");
        assertThat(output.getOut()).contains("36");
    }

    @Test
    void doesNotRaiseAlarmWhenTemperatureWithinThreshold(CapturedOutput output) {
        service.evaluate(new SensorMeasurement("warehouse-1", "t1", SensorType.TEMPERATURE, 30));

        assertThat(output.getOut()).doesNotContain("ALARM");
    }

    @Test
    void raisesAlarmWhenHumidityExceedsThreshold(CapturedOutput output) {
        service.evaluate(new SensorMeasurement("warehouse-1", "h1", SensorType.HUMIDITY, 55));

        assertThat(output.getOut()).contains("ALARM");
        assertThat(output.getOut()).contains("h1");
        assertThat(output.getOut()).contains("55");
    }

    @Test
    void doesNotRaiseAlarmWhenHumidityEqualsThreshold(CapturedOutput output) {
        service.evaluate(new SensorMeasurement("warehouse-1", "h1", SensorType.HUMIDITY, 50));

        assertThat(output.getOut()).doesNotContain("ALARM");
    }
}
