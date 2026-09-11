package app.central

import app.central.actor.AlarmReporter
import app.central.actor.WarehouseMonitor
import app.central.settings.CentralSettings
import app.central.stream.MeasurementStream
import app.common.nats.NatsGuardian
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem

fun main() {
    val config = ConfigFactory.load()
    val settings = CentralSettings(config)

    val guardian = NatsGuardian.create { context, connection ->
        val reporter = context.spawn(
            AlarmReporter.create(settings.thresholds),
            "alarm-reporter"
        )
        val monitor = context.spawn(
            WarehouseMonitor.create(settings.thresholds, reporter),
            "warehouse-monitor"
        )
        MeasurementStream.start(connection, settings.measurementSubject, monitor, context.system)
    }

    ActorSystem.create(guardian, "central", config)
}
