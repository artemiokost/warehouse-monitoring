package app.central.stream

import app.central.actor.WarehouseMonitor.Observe
import app.common.protocol.Measurement
import io.nats.client.Connection
import io.nats.client.Message
import java.time.Duration
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.javadsl.Sink
import org.apache.pekko.stream.javadsl.Source
import org.apache.pekko.stream.typed.javadsl.ActorFlow
import org.slf4j.LoggerFactory

object MeasurementStream {

    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    private const val BUFFER_SIZE = 1024

    private val OBSERVE_TIMEOUT = Duration.ofSeconds(5)

    fun start(
        connection: Connection,
        subject: String,
        monitor: ActorRef<Observe>,
        system: ActorSystem<*>,
    ) {
        val flow = ActorFlow.ask(1, monitor, OBSERVE_TIMEOUT) { measurement, replyTo ->
            Observe(measurement, replyTo)
        }
        val inbox = Source.queue<Message>(BUFFER_SIZE, OverflowStrategy.dropHead())
            .mapConcat { message -> listOfNotNull(decode(message)) }
            .via(flow)
            .log("measurement-stream")
            .to(Sink.ignore())
            .run(system)

        connection
            .createDispatcher { message -> inbox.offer(message) }
            .subscribe(subject)

        logger.info("Subscribed to {}", subject)
    }

    private fun decode(message: Message): Measurement? {
        val measurement = Measurement.decode(message.data)

        if (measurement == null) {
            logger.warn("Discarded malformed message on {}", message.subject)
        }

        return measurement
    }
}
