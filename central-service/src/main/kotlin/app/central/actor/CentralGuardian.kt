package app.central.actor

import app.central.config.CentralConfig
import app.central.stream.MeasurementIngress
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import java.time.Duration
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.javadsl.Behaviors

object CentralGuardian {

    private val RECONNECT_WAIT = Duration.ofSeconds(1)

    fun create(): Behavior<Void> = Behaviors.setup { context ->
        val config = CentralConfig.from(context.system.settings().config())
        val reporter = context.spawn(AlarmReporter.create(), "alarm-reporter")
        val registry = context.spawn(WarehouseRegistry.create(config.thresholds, reporter), "warehouse-registry")
        val connection = connect(config.natsUrl)

        MeasurementIngress.start(connection, registry, context.system)

        Behaviors.receiveSignal { _, signal ->
            if (signal is PostStop) {
                connection.close()
            }
            Behaviors.same()
        }
    }

    private fun connect(natsUrl: String): Connection = Nats.connect(
        Options.Builder()
            .server(natsUrl)
            .reconnectWait(RECONNECT_WAIT)
            .maxReconnects(-1)
            .build()
    )
}
