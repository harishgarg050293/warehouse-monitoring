package com.warehouse.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WarehouseMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseMonitoringApplication.class, args);
    }
}
