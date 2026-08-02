package com.warehouse.monitoring.monitoring;

import com.warehouse.monitoring.broker.BrokerConfig;
import com.warehouse.monitoring.model.SensorMeasurement;
import com.warehouse.monitoring.model.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

/**
 * Central monitoring service: consumes warehouse measurements and raises alarms when
 * configured thresholds are exceeded.
 */
@Service
public class CentralMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(CentralMonitoringService.class);

    private final ThresholdConfig thresholds;

    public CentralMonitoringService(ThresholdConfig thresholds) {
        this.thresholds = thresholds;
    }

    @JmsListener(destination = BrokerConfig.SENSOR_MEASUREMENTS_QUEUE)
    public void onMeasurement(SensorMeasurement measurement) {
        log.info("Received measurement warehouseId={} sensorId={} type={} value={}",
                measurement.warehouseId(),
                measurement.sensorId(),
                measurement.type(),
                measurement.value());

        evaluate(measurement);
    }

    void evaluate(SensorMeasurement measurement) {
        double threshold = thresholdFor(measurement.type());
        if (measurement.value() > threshold) {
            raiseAlarm(measurement, threshold);
        }
    }

    private double thresholdFor(SensorType type) {
        return switch (type) {
            case TEMPERATURE -> thresholds.temperature();
            case HUMIDITY -> thresholds.humidity();
        };
    }

    private void raiseAlarm(SensorMeasurement measurement, double threshold) {
        log.error("ALARM: {} sensor {} value={} exceeds threshold {}",
                measurement.type().name().toLowerCase(),
                measurement.sensorId(),
                measurement.value(),
                threshold);
    }
}
