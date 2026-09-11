package app.warehouse

import app.common.nats.NatsGuardian
import app.warehouse.settings.WarehouseSettings
import app.warehouse.stream.MeasurementPublisher
import app.warehouse.stream.SensorStream
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem

fun main() {
    val config = ConfigFactory.load()
    val settings = WarehouseSettings(config)

    val guardian = NatsGuardian.create { context, connection ->
        context.log.info("Publishing to {}", settings.measurementSubject)

        settings.sensors.forEach { sensor ->
            SensorStream.source(sensor, settings.id, context.system)
                .to(MeasurementPublisher.sink(connection, settings.measurementSubject))
                .run(context.system)
        }
    }

    ActorSystem.create(guardian, "warehouse", config)
}
