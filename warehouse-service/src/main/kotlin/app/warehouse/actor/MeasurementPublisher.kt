package app.warehouse.actor

import app.protocol.Measurement
import app.protocol.MeasurementCodec
import app.protocol.Subjects
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import java.time.Duration
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.SupervisorStrategy
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

class MeasurementPublisher private constructor(
    context: ActorContext<Command>,
    private val connection: Connection,
) : AbstractBehavior<MeasurementPublisher.Command>(context) {

    sealed interface Command

    data class Publish(val measurement: Measurement) : Command

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
        .onMessage(Publish::class.java, ::onPublish)
        .onSignal(PostStop::class.java, ::onPostStop)
        .build()

    private fun onPostStop(signal: PostStop): Behavior<Command> {
        connection.close()
        return this
    }

    private fun onPublish(command: Publish): Behavior<Command> {
        val measurement = command.measurement
        connection.publish(Subjects.measurement(measurement.warehouseId), MeasurementCodec.encode(measurement))
        return this
    }

    companion object {
        private val MAX_BACKOFF = Duration.ofSeconds(30)
        private val MIN_BACKOFF = Duration.ofSeconds(1)
        private val RECONNECT_WAIT = Duration.ofSeconds(1)
        private const val RANDOM_FACTOR = 0.2

        fun create(natsUrl: String): Behavior<Command> = Behaviors
            .supervise(Behaviors.setup<Command> { context -> MeasurementPublisher(context, connect(natsUrl)) })
            .onFailure(SupervisorStrategy.restartWithBackoff(MIN_BACKOFF, MAX_BACKOFF, RANDOM_FACTOR))

        private fun connect(natsUrl: String): Connection = Nats.connect(
            Options.Builder()
                .server(natsUrl)
                .reconnectWait(RECONNECT_WAIT)
                .maxReconnects(-1)
                .build()
        )
    }
}
