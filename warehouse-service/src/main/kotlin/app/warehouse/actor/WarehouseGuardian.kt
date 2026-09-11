package app.warehouse.actor

import app.warehouse.config.WarehouseConfig
import app.warehouse.stream.SensorIngress
import io.nats.client.Connection
import io.nats.client.ConnectionListener.Events
import io.nats.client.Nats
import io.nats.client.Options
import java.time.Duration
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors

object WarehouseGuardian {

    sealed interface Command

    data class Connected(val connection: Connection) : Command

    private val RECONNECT_WAIT = Duration.ofSeconds(1)

    /** First contact comes as CONNECTED, or as RECONNECTED when the broker was down at startup. */
    private val READY = setOf(Events.CONNECTED, Events.RECONNECTED)

    fun create(): Behavior<Command> = Behaviors.setup { context ->
        val config = WarehouseConfig.from(context.system.settings().config())

        connect(config.natsUrl) { connection -> context.self.tell(Connected(connection)) }
        context.log.info("Connecting to {}", config.natsUrl)

        Behaviors.receive(Command::class.java)
            .onMessage(Connected::class.java) { connected -> run(context, config, connected.connection) }
            .build()
    }

    private fun run(context: ActorContext<Command>, config: WarehouseConfig, connection: Connection): Behavior<Command> {
        val publisher = context.spawn(MeasurementPublisher.create(connection), "measurement-publisher")

        config.sensors.forEach { binding ->
            SensorIngress.start(binding, config.id, publisher, context.system)
        }

        return Behaviors.receive(Command::class.java)
            .onMessage(Connected::class.java) { Behaviors.same() } // later reconnects are the client's business
            .onSignal(PostStop::class.java) { connection.close(); Behaviors.same() }
            .build()
    }

    /** Connects on a client thread and retries forever; the guardian is told every time the broker answers. */
    private fun connect(natsUrl: String, onConnected: (Connection) -> Unit) = Nats.connectAsynchronously(
        Options.Builder()
            .server(natsUrl)
            .reconnectWait(RECONNECT_WAIT)
            .maxReconnects(-1)
            .connectionListener { connection, event -> if (event in READY) onConnected(connection) }
            .build(),
        true,
    )
}
