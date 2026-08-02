package com.warehouse.monitoring.warehouse;

import com.warehouse.monitoring.model.SensorType;
import com.warehouse.monitoring.parse.MeasurementParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UdpSensorListener {

    private static final Logger log = LoggerFactory.getLogger(UdpSensorListener.class);

    private final MeasurementParser parser;
    private final WarehousePublisher publisher;
    private final List<WarehousesProperties.WarehouseConfig> warehouses;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<DatagramSocket> sockets = new ArrayList<>();
    private ExecutorService executor;

    public UdpSensorListener(
            MeasurementParser parser,
            WarehousePublisher publisher,
            WarehousesProperties warehousesProperties) {
        this.parser = parser;
        this.publisher = publisher;
        this.warehouses = warehousesProperties.warehouses();
    }

    @PostConstruct
    public void start() throws SocketException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        executor = Executors.newVirtualThreadPerTaskExecutor();

        for (WarehousesProperties.WarehouseConfig warehouse : warehouses) {
            for (WarehousesProperties.SensorConfig sensor : warehouse.sensors()) {
                bindAndListen(warehouse.id(), sensor.port(), sensor.type());
            }
        }
        log.info("Listening for warehouses: {}", warehouses);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        for (DatagramSocket socket : sockets) {
            socket.close();
        }
        sockets.clear();
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void bindAndListen(String warehouseId, int port, SensorType type) throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);
        sockets.add(socket);
        executor.submit(() -> listenLoop(socket, warehouseId, type));
    }

    private void listenLoop(DatagramSocket socket, String warehouseId, SensorType type) {
        byte[] buffer = new byte[1024];
        while (running.get() && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String payload = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8)
                        .trim();
                handlePayload(payload, warehouseId, type);
            } catch (SocketException e) {
                if (running.get()) {
                    log.warn("UDP socket error on {} for {}: {}", type, warehouseId, e.getMessage());
                }
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("Failed to receive UDP packet for {} in {}: {}", type, warehouseId, e.getMessage());
                }
            } catch (Exception e) {
                log.error("Unexpected error while processing {} measurement for {}", type, warehouseId, e);
            }
        }
    }

    void handlePayload(String payload, String warehouseId, SensorType type) {
        parser.parse(payload, type, warehouseId).ifPresentOrElse(
                publisher::publish,
                () -> log.warn("Ignoring invalid {} payload for {}: '{}'", type, warehouseId, payload)
        );
    }
}
