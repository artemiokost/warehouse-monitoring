# warehouse-monitoring

UDP sensors, a warehouse service per building, NATS in between, and a central service that raises an
alarm when a reading crosses its threshold.

Kotlin, JDK 25, Apache Pekko 1.7, NATS, Gradle 9.7.

## Run

```bash
docker compose up -d
```

```bash
./gradlew :central-service:run
```

```bash
./gradlew :warehouse-service:run
```

One terminal per service. NATS listens on 4222. Start order does not matter: both services log
`Connecting to`, wait for the broker and come up once it answers.

Stop:

```bash
./gradlew --stop
```

```bash
docker compose down
```

## Send readings

```bash
echo -n "sensor_id=t1; value=41"   | nc -u -w1 127.0.0.1 3344
echo -n "sensor_id=t1; value=44"   | nc -u -w1 127.0.0.1 3344
echo -n "sensor_id=t1; value=20"   | nc -u -w1 127.0.0.1 3344
echo -n "sensor_id=h1; value=73.5" | nc -u -w1 127.0.0.1 3355
```

The central service prints:

```
ALARM   warehouse=w1 sensor=t1 TEMPERATURE=41.0°C threshold=35.0°C measuredAt=2026-09-12T05:00:38Z
CLEARED warehouse=w1 sensor=t1 TEMPERATURE=20.0°C threshold=35.0°C measuredAt=2026-09-12T05:00:43Z
ALARM   warehouse=w1 sensor=h1 HUMIDITY=73.5% threshold=50.0% measuredAt=2026-09-12T05:00:45Z
```

`44` prints nothing: the alarm fires on the crossing, not on every reading above the limit. Exactly
`35` is not a crossing. Anything that does not parse is logged and dropped.

## Modules

| Module | Contains |
|---|---|
| `common` | `Measurement` and its JSON codec, the sensor datagram syntax, the NATS guardian |
| `warehouse-service` | binds the sensor ports, parses readings, publishes to NATS |
| `central-service` | subscribes to every warehouse, keeps alarm state per warehouse and sensor, raises and clears alarms |

The warehouse stream publishes from a `Sink.foreach`, one datagram at a time. The central stream
hands each measurement to an actor with `ActorFlow.ask` and takes the next only after the reply.
Nothing on the data path is buffered without a bound.

## Configuration

Defaults live in `src/main/resources/application.conf` of each service.

| Variable | Service | Default |
|---|---|---|
| `WAREHOUSE_ID` | warehouse | `w1` |
| `TEMPERATURE_PORT` | warehouse | `3344` |
| `HUMIDITY_PORT` | warehouse | `3355` |
| `NATS_URL` | both | `nats://127.0.0.1:4222` |
| `MEASUREMENT_SUBJECT` | warehouse | `warehouse.{id}.measurement` |
| `MEASUREMENT_SUBJECT` | central | `warehouse.*.measurement` |

Thresholds are in `central-service/src/main/resources/application.conf`.

A blank override, or a `WAREHOUSE_ID` containing whitespace, `.`, `*` or `>`, stops the service on
startup and names the setting.

A second warehouse on the same machine:

```bash
WAREHOUSE_ID=w2 TEMPERATURE_PORT=4344 HUMIDITY_PORT=4355 ./gradlew :warehouse-service:run
```

One central service covers them all.
