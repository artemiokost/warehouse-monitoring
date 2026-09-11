package app.central.actor

import app.common.protocol.Measurement
import app.common.protocol.SensorType
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.Behaviors

object AlarmReporter {

    sealed interface Command

    data class Raised(val measurement: Measurement) : Command

    data class Cleared(val measurement: Measurement) : Command

    fun create(thresholds: Map<SensorType, Double>): Behavior<Command> = Behaviors.receive { context, command ->
        when (command) {
            is Raised -> context.log.warn("ALARM {}", command.measurement.describe(thresholds))
            is Cleared -> context.log.info("CLEARED {}", command.measurement.describe(thresholds))
        }
        Behaviors.same()
    }
}
