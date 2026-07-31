package com.warehouse.monitoring.warehouse;

import com.warehouse.monitoring.broker.BrokerConfig;
import com.warehouse.monitoring.model.SensorMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class WarehousePublisher {

    private static final Logger log = LoggerFactory.getLogger(WarehousePublisher.class);

    private final JmsTemplate jmsTemplate;

    public WarehousePublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publish(SensorMeasurement measurement) {
        jmsTemplate.convertAndSend(BrokerConfig.SENSOR_MEASUREMENTS_QUEUE, measurement);
        log.info("Published measurement warehouseId={} sensorId={} type={} value={}",
                measurement.warehouseId(),
                measurement.sensorId(),
                measurement.type(),
                measurement.value());
    }
}
