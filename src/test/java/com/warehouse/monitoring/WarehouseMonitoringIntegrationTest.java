package com.warehouse.monitoring;

import com.warehouse.monitoring.broker.BrokerConfig;
import com.warehouse.monitoring.model.SensorMeasurement;
import com.warehouse.monitoring.model.SensorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class WarehouseMonitoringIntegrationTest {

    private static final int W1_TEMP_PORT = 13344;
    private static final int W1_HUM_PORT = 13355;
    private static final int W2_TEMP_PORT = 23444;
    private static final int W2_HUM_PORT = 23555;

    @DynamicPropertySource
    static void warehouseConfig(DynamicPropertyRegistry registry) {
        registry.add("warehouses[0].id", () -> "warehouse-1");
        registry.add("warehouses[0].sensors[0].type", () -> "temperature");
        registry.add("warehouses[0].sensors[0].port", () -> W1_TEMP_PORT);
        registry.add("warehouses[0].sensors[1].type", () -> "humidity");
        registry.add("warehouses[0].sensors[1].port", () -> W1_HUM_PORT);

        registry.add("warehouses[1].id", () -> "warehouse-2");
        registry.add("warehouses[1].sensors[0].type", () -> "temperature");
        registry.add("warehouses[1].sensors[0].port", () -> W2_TEMP_PORT);
        registry.add("warehouses[1].sensors[1].type", () -> "humidity");
        registry.add("warehouses[1].sensors[1].port", () -> W2_HUM_PORT);
    }

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void publishesViaBrokerAndRaisesAlarmForHighTemperature(CapturedOutput output) {
        SensorMeasurement measurement =
                new SensorMeasurement("warehouse-1", "t1", SensorType.TEMPERATURE, 42);

        jmsTemplate.convertAndSend(BrokerConfig.SENSOR_MEASUREMENTS_QUEUE, measurement);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(output.getOut()).contains("ALARM").contains("t1").contains("42"));
    }

    @Test
    void udpTemperaturePacketIsProcessedAndAlarmsWhenAboveThreshold(CapturedOutput output) throws Exception {
        sendUdp(W1_TEMP_PORT, "sensor_id=t1; value=40");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(output.getOut())
                        .contains("Published measurement")
                        .contains("warehouseId=warehouse-1")
                        .contains("ALARM")
                        .contains("40"));
    }

    @Test
    void udpHumidityBelowThresholdDoesNotAlarm(CapturedOutput output) throws Exception {
        sendUdp(W1_HUM_PORT, "sensor_id=h1; value=40");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(output.getOut()).contains("Published measurement").contains("h1"));

        TimeUnit.MILLISECONDS.sleep(500);
        assertThat(output.getOut()).doesNotContain("ALARM: humidity");
    }

    @Test
    void multipleWarehousesAreMonitoredByCentralService(CapturedOutput output) {
        jmsTemplate.convertAndSend(
                BrokerConfig.SENSOR_MEASUREMENTS_QUEUE,
                new SensorMeasurement("warehouse-1", "t1", SensorType.TEMPERATURE, 40));
        jmsTemplate.convertAndSend(
                BrokerConfig.SENSOR_MEASUREMENTS_QUEUE,
                new SensorMeasurement("warehouse-2", "t2", SensorType.TEMPERATURE, 42));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(output.getOut()).contains("warehouseId=warehouse-1");
            assertThat(output.getOut()).contains("warehouseId=warehouse-2");
            assertThat(output.getOut()).contains("ALARM: warehouse=warehouse-1 temperature sensor t1");
            assertThat(output.getOut()).contains("ALARM: warehouse=warehouse-2 temperature sensor t2");
        });
    }

    @Test
    void udpPacketFromSecondWarehouseIsTaggedCorrectly(CapturedOutput output) throws Exception {
        sendUdp(W2_TEMP_PORT, "sensor_id=t2; value=36");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(output.getOut())
                        .contains("warehouseId=warehouse-2")
                        .contains("ALARM: warehouse=warehouse-2 temperature sensor t2"));
    }

    private static void sendUdp(int port, String payload) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getLoopbackAddress(), port);
            socket.send(packet);
        }
    }
}
