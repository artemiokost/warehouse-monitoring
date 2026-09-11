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

    data class Track(val measurement: Measurement) : Command

    private val monitors = mutableMapOf<String, ActorRef<WarehouseMonitor.Command>>()

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
        .onMessage(Track::class.java, ::onTrack)
        .build()

    private fun onTrack(command: Track): Behavior<Command> {
        val warehouseId = command.measurement.warehouseId

        monitors
            .getOrPut(warehouseId) {
                context.spawn(WarehouseMonitor.create(warehouseId, thresholds, reporter), "warehouse-$warehouseId")
            }
            .tell(WarehouseMonitor.Observe(command.measurement))

        return this
    }

    companion object {
        fun create(thresholds: Thresholds, reporter: ActorRef<AlarmReporter.Command>): Behavior<Command> =
            Behaviors.setup { context -> WarehouseRegistry(context, thresholds, reporter) }
    }
}
