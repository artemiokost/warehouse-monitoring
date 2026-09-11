package app.central.stream

import app.central.actor.WarehouseRegistry
import app.protocol.Measurement
import app.protocol.MeasurementCodec
import app.protocol.Subjects
import io.nats.client.Connection
import io.nats.client.Message
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.javadsl.Source
import org.apache.pekko.stream.typed.javadsl.ActorSink
import org.slf4j.Logger

object MeasurementIngress {

    private const val BUFFER_SIZE = 1024

    fun start(
        connection: Connection,
        registry: ActorRef<WarehouseRegistry.Command>,
        system: ActorSystem<*>,
    ) {
        // The broker pushes, the actors pull. This queue is where the two meet and the only place a
        // measurement can be lost: when the buffer is full the oldest reading gives way to the newest.
        val inbox = Source.queue<Message>(BUFFER_SIZE, OverflowStrategy.dropHead())
            .mapConcat { message -> listOfNotNull(decode(message, system.log())) }
            .to(
                ActorSink.actorRefWithBackpressure(
                    registry,
                    { ackTo, measurement -> WarehouseRegistry.Track(measurement, ackTo) },
                    { ackTo -> WarehouseRegistry.IngressStarted(ackTo) },
                    WarehouseRegistry.Ack,
                    WarehouseRegistry.IngressStopped(null),
                    { cause -> WarehouseRegistry.IngressStopped(cause) },
                )
            )
            .run(system)

        connection.createDispatcher { message -> inbox.offer(message) }.subscribe(Subjects.EVERY_MEASUREMENT)

        system.log().info("Subscribed to {}", Subjects.EVERY_MEASUREMENT)
    }

    private fun decode(message: Message, log: Logger): Measurement? {
        val measurement = MeasurementCodec.decode(message.data)

        if (measurement == null) {
            log.warn("Discarded malformed message on {}", message.subject)
        }

        return measurement
    }
}
