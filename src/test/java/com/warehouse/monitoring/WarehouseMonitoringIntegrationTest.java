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

    private static final int TEMP_PORT = 13344;
    private static final int HUM_PORT = 13355;

    @DynamicPropertySource
    static void udpPorts(DynamicPropertyRegistry registry) {
        registry.add("warehouse.udp.temperature-port", () -> TEMP_PORT);
        registry.add("warehouse.udp.humidity-port", () -> HUM_PORT);
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
        sendUdp(TEMP_PORT, "sensor_id=t1; value=40");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(output.getOut())
                        .contains("Published measurement")
                        .contains("ALARM")
                        .contains("40"));
    }

    @Test
    void udpHumidityBelowThresholdDoesNotAlarm(CapturedOutput output) throws Exception {
        sendUdp(HUM_PORT, "sensor_id=h1; value=40");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(output.getOut()).contains("Published measurement").contains("h1"));

        // Give listener a moment; ensure no alarm for this value
        TimeUnit.MILLISECONDS.sleep(500);
        assertThat(output.getOut()).doesNotContain("ALARM: humidity");
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
