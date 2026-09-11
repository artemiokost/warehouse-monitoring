package app.central.actor

import app.central.alarm.Alarm
import app.central.alarm.Thresholds
import app.protocol.Measurement
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

class WarehouseMonitor private constructor(
    context: ActorContext<Command>,
    private val warehouseId: String,
    private val thresholds: Thresholds,
    private val reporter: ActorRef<AlarmReporter.Command>,
) : AbstractBehavior<WarehouseMonitor.Command>(context) {

    sealed interface Command

    data class Observe(val measurement: Measurement, val ackTo: ActorRef<WarehouseRegistry.Ack>) : Command

    private val breaching = mutableSetOf<String>()

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
        .onMessage(Observe::class.java, ::onObserve)
        .build()

    private fun onObserve(command: Observe): Behavior<Command> {
        val measurement = command.measurement
        val threshold = thresholds.limitOf(measurement.kind)
        val alarm = Alarm(warehouseId, measurement.sensorId, measurement.kind, measurement.value, threshold, measurement.at)

        when {
            measurement.value > threshold && breaching.add(measurement.sensorId) ->
                reporter.tell(AlarmReporter.Raised(alarm))

            measurement.value <= threshold && breaching.remove(measurement.sensorId) ->
                reporter.tell(AlarmReporter.Cleared(alarm))
        }

        command.ackTo.tell(WarehouseRegistry.Ack)
        return this
    }

    companion object {
        fun create(
            warehouseId: String,
            thresholds: Thresholds,
            reporter: ActorRef<AlarmReporter.Command>,
        ): Behavior<Command> = Behaviors.setup { context ->
            WarehouseMonitor(context, warehouseId, thresholds, reporter)
        }
    }
}
