# Warehouse Monitoring Service

Reactive warehouse monitoring system that collects temperature and humidity sensor readings over UDP, publishes them through an embedded ActiveMQ Artemis message broker, and raises console alarms when configured thresholds are exceeded.

## Architecture

```
Temperature sensor (UDP 3344) ──┐
                                ├──▶ Warehouse Service ──▶ Artemis queue ──▶ Central Monitoring ──▶ ALARM logs
Humidity sensor    (UDP 3355) ──┘
```

- **Warehouse Service** – listens on UDP ports, parses sensor payloads, publishes to the broker.
- **Embedded ActiveMQ Artemis** – decouples warehouse collection from central monitoring.
- **Central Monitoring Service** – consumes measurements and raises alarms when thresholds are crossed.

## Requirements

- JDK 21+
- No external broker or Docker required (Artemis runs in-process)

## Run

```bash
./gradlew bootRun
```

On Windows:

```bat
gradlew.bat bootRun
```

Expected startup log includes UDP listeners on ports `3344` (temperature) and `3355` (humidity).

## Simulate sensors

### Java sensor simulator

With the app running (`gradlew.bat bootRun`), send readings from another terminal:

```bat
./gradlew.bat sendSensor --args="warehouse-1 temperature t1 30"
./gradlew.bat sendSensor --args="warehouse-1 temperature t1 36"
./gradlew.bat sendSensor --args="warehouse-2 temperature t2 36"
./gradlew.bat sendSensor --args="warehouse-2 humidity h2 55"
```

Arguments: `<warehouse> <temperature|humidity> <sensorId> <value> [host]`

### Manual UDP (payload format from the assignment)

```
sensor_id=t1; value=30
sensor_id=h1; value=40
```

## Thresholds (defaults)

| Warehouse   | Sensor      | UDP port | Alarm when value > |
|-------------|-------------|----------|--------------------|
| warehouse-1 | Temperature | 3344     | 35 °C              |
| warehouse-1 | Humidity    | 3355     | 50 %               |
| warehouse-2 | Temperature | 4344     | 35 °C              |
| warehouse-2 | Humidity    | 4355     | 50 %               |

Warehouses and sensors are configured under `warehouses` in `application.yml`. To add another warehouse, add a new list entry with a unique `id` and non-conflicting sensor ports.

## Example alarm

```
ALARM: warehouse=warehouse-1 temperature sensor t1 value=36 exceeds threshold 35
```

## Tests

```bash
./gradlew test
```

Coverage includes:

- UDP payload parsing
- Threshold / alarm evaluation
- End-to-end flow via broker and UDP

## Design notes

- One JVM listens for all configured warehouses; each UDP port is tied to a warehouse id and sensor type.
- JSON message conversion is used on the Artemis queue `sensor.measurements`.
- Central monitoring applies shared thresholds to measurements from every warehouse.
