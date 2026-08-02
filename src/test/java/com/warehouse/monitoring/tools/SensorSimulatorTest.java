package com.warehouse.monitoring.tools;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SensorSimulatorTest {

    @Test
    void sendsUdpPayload() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0)) {
            int port = server.getLocalPort();
            byte[] buffer = new byte[1024];

            SensorSimulator.send("127.0.0.1", port, "sensor_id=t1; value=30");

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            server.setSoTimeout(2000);
            server.receive(packet);

            String payload = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
            assertThat(payload).isEqualTo("sensor_id=t1; value=30");
        }
    }
}
