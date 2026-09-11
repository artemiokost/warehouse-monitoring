package app.central.actor

import app.central.alarm.Thresholds
import app.protocol.Measurement
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

class WarehouseRegistry private constructor(
    context: ActorContext<Command>,
    private val thresholds: Thresholds,
    private val reporter: ActorRef<AlarmReporter.Command>,
) : AbstractBehavior<WarehouseRegistry.Command>(context) {

    sealed interface Command

    /** Demand signal back to the ingress stream, sent by the monitor once the measurement is applied. */
    data object Ack

    data class IngressStarted(val ackTo: ActorRef<Ack>) : Command

    data class IngressStopped(val cause: Throwable?) : Command

    data class Track(val measurement: Measurement, val ackTo: ActorRef<Ack>) : Command

    private val monitors = mutableMapOf<String, ActorRef<WarehouseMonitor.Command>>()

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
        .onMessage(IngressStarted::class.java, ::onIngressStarted)
        .onMessage(IngressStopped::class.java, ::onIngressStopped)
        .onMessage(Track::class.java, ::onTrack)
        .build()

    private fun onIngressStarted(command: IngressStarted): Behavior<Command> {
        command.ackTo.tell(Ack)
        return this
    }

    private fun onIngressStopped(command: IngressStopped): Behavior<Command> {
        context.log.error("Measurement ingress stopped", command.cause)
        return this
    }

    private fun onTrack(command: Track): Behavior<Command> {
        val warehouseId = command.measurement.warehouseId

        monitors
            .getOrPut(warehouseId) {
                context.spawn(WarehouseMonitor.create(warehouseId, thresholds, reporter), "warehouse-$warehouseId")
            }
            .tell(WarehouseMonitor.Observe(command.measurement, command.ackTo))

        return this
    }

    companion object {
        fun create(thresholds: Thresholds, reporter: ActorRef<AlarmReporter.Command>): Behavior<Command> =
            Behaviors.setup { context -> WarehouseRegistry(context, thresholds, reporter) }
    }
}
