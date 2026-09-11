package app.common.nats

import app.common.config.getNonBlankString
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import java.time.Duration
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.slf4j.LoggerFactory

object NatsGuardian {

    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    private val RECONNECT_WAIT = Duration.ofSeconds(1)

    fun create(
        start: (ActorContext<Void>, Connection) -> Unit
    ): Behavior<Void> = Behaviors.setup { context ->
        val connection = connect(context.system.settings().config().getNonBlankString("nats.url"))

        start(context, connection)

        Behaviors.receiveSignal { _, signal ->
            if (signal is PostStop) connection.close()
            Behaviors.same()
        }
    }

    private fun connect(url: String): Connection {
        logger.info("Connecting to {}", url)

        val connection = Nats.connectReconnectOnConnect(
            Options.Builder()
                .server(url)
                .reconnectWait(RECONNECT_WAIT)
                .maxReconnects(-1)
                .build(),
        )

        logger.info("Connected to {}", url)

        return connection
    }
}
