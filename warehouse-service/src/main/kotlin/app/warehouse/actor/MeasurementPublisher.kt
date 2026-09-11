package app.warehouse.actor

import app.protocol.Measurement
import app.protocol.MeasurementCodec
import app.protocol.Subjects
import io.nats.client.Connection
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

class MeasurementPublisher private constructor(
    context: ActorContext<Command>,
    private val connection: Connection,
) : AbstractBehavior<MeasurementPublisher.Command>(context) {

    sealed interface Command

    /** Demand signal back to the sensor stream: the next measurement may be sent. */
    data object Ack

    data class IngressStarted(val ackTo: ActorRef<Ack>) : Command

    data class IngressStopped(val cause: Throwable?) : Command

    data class Publish(val measurement: Measurement, val ackTo: ActorRef<Ack>) : Command

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
        .onMessage(IngressStarted::class.java, ::onIngressStarted)
        .onMessage(IngressStopped::class.java, ::onIngressStopped)
        .onMessage(Publish::class.java, ::onPublish)
        .build()

    private fun onIngressStarted(command: IngressStarted): Behavior<Command> {
        command.ackTo.tell(Ack)
        return this
    }

    private fun onIngressStopped(command: IngressStopped): Behavior<Command> {
        context.log.error("Sensor ingress stopped", command.cause)
        return this
    }

    private fun onPublish(command: Publish): Behavior<Command> {
        val measurement = command.measurement

        // An element that is never acknowledged stalls its stream, so the ack is unconditional: a
        // measurement the client cannot buffer (broker unreachable for long) is dropped, not retried.
        runCatching { connection.publish(Subjects.measurement(measurement.warehouseId), MeasurementCodec.encode(measurement)) }
            .onFailure { context.log.warn("Dropped measurement from {}: {}", measurement.sensorId, it.toString()) }
        command.ackTo.tell(Ack)

        return this
    }

    companion object {
        fun create(connection: Connection): Behavior<Command> =
            Behaviors.setup { context -> MeasurementPublisher(context, connection) }
    }
}
