package app.central.actor

import app.common.protocol.Measurement
import app.common.protocol.Sensor
import app.common.protocol.SensorType
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.Behaviors

object WarehouseMonitor {

    data class Observe(val measurement: Measurement, val replyTo: ActorRef<Done>)

    fun create(
        thresholds: Map<SensorType, Double>,
        reporter: ActorRef<AlarmReporter.Command>,
    ): Behavior<Observe> = Behaviors.setup {
        val breaching = mutableSetOf<Sensor>()

        Behaviors.receiveMessage { (measurement, replyTo) ->
            val threshold = thresholds.getValue(measurement.sensor.type)

            when {
                measurement.value > threshold && breaching.add(measurement.sensor) -> {
                    reporter.tell(AlarmReporter.Raised(measurement))
                }
                measurement.value <= threshold && breaching.remove(measurement.sensor) -> {
                    reporter.tell(AlarmReporter.Cleared(measurement))
                }
            }

            replyTo.tell(Done.done())
            Behaviors.same()
        }
    }
}
