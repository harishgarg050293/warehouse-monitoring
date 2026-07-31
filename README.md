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

### PowerShell helper

```powershell
.\tools\send-sensor.ps1 -Type temperature -SensorId t1 -Value 30
.\tools\send-sensor.ps1 -Type temperature -SensorId t1 -Value 36
.\tools\send-sensor.ps1 -Type humidity -SensorId h1 -Value 40
.\tools\send-sensor.ps1 -Type humidity -SensorId h1 -Value 55
```

### Manual UDP (payload format from the assignment)

```
sensor_id=t1; value=30
sensor_id=h1; value=40
```

## Thresholds (defaults)

| Sensor      | UDP port | Alarm when value > |
|-------------|----------|--------------------|
| Temperature | 3344     | 35 °C              |
| Humidity    | 3355     | 50 %               |

Configure in `src/main/resources/application.yml` under `monitoring.thresholds` and `warehouse.udp`.

## Example alarm

```
ALARM: temperature sensor 't1' in warehouse 'warehouse-1' reported value=36.0°C which exceeds threshold 35.0°C
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

- Sensor intake uses dedicated UDP listener threads; measurements are published asynchronously through JMS.
- JSON message conversion is used on the Artemis queue `sensor.measurements`.
- Multiple warehouses can be represented via `warehouse.id` (default `warehouse-1`); the central service monitors all published measurements against shared thresholds.
