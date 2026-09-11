package app.warehouse.actor

import app.warehouse.config.WarehouseConfig
import app.warehouse.stream.SensorIngress
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.Behaviors

object WarehouseGuardian {

    fun create(): Behavior<Void> = Behaviors.setup { context ->
        val config = WarehouseConfig.from(context.system.settings().config())
        val publisher = context.spawn(MeasurementPublisher.create(config.natsUrl), "measurement-publisher")

        config.sensors.forEach { binding ->
            SensorIngress.start(binding, config.id, publisher, context.system)
        }

        Behaviors.empty()
    }
}
