package com.warehouse.monitoring.tools;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sends a simulated sensor reading over UDP.
 *
 * <p>Usage:
 * {@code gradlew sendSensor --args="warehouse-1 temperature t1 30"}
 */
public final class SensorSimulator {

    private static final Map<String, Map<String, Integer>> PORTS = Map.of(
            "warehouse-1", Map.of("temperature", 3344, "humidity", 3355),
            "warehouse-2", Map.of("temperature", 4344, "humidity", 4355)
    );

    private SensorSimulator() {
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            printUsage();
            System.exit(1);
        }

        String warehouse = args[0];
        String type = args[1].toLowerCase();
        String sensorId = args[2];
        double value = Double.parseDouble(args[3]);
        String host = args.length > 4 ? args[4] : "127.0.0.1";

        Map<String, Integer> warehousePorts = PORTS.get(warehouse);
        if (warehousePorts == null) {
            System.err.println("Unknown warehouse: " + warehouse);
            printUsage();
            System.exit(1);
        }

        Integer port = warehousePorts.get(type);
        if (port == null) {
            System.err.println("Unknown sensor type: " + type);
            printUsage();
            System.exit(1);
        }

        String valueText = value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        String payload = "sensor_id=" + sensorId + "; value=" + valueText;
        send(host, port, payload);
        System.out.printf("Sent to %s:%d (%s) -> %s%n", host, port, warehouse, payload);
    }

    static void send(String host, int port, String payload) {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getByName(host), port);
            socket.send(packet);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send UDP packet", e);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: SensorSimulator <warehouse> <temperature|humidity> <sensorId> <value> [host]");
        System.err.println("Example: SensorSimulator warehouse-1 temperature t1 36");
        System.err.println("         SensorSimulator warehouse-2 humidity h2 55");
    }
}
