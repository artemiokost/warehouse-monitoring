package app.central.actor

import app.central.alarm.Alarm
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

class AlarmReporter private constructor(
    context: ActorContext<Command>,
) : AbstractBehavior<AlarmReporter.Command>(context) {

    sealed interface Command

    data class Cleared(val alarm: Alarm) : Command

    data class Raised(val alarm: Alarm) : Command

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
        .onMessage(Cleared::class.java, ::onCleared)
        .onMessage(Raised::class.java, ::onRaised)
        .build()

    private fun onCleared(command: Cleared): Behavior<Command> {
        context.log.info("CLEARED {}", command.alarm.summary)
        return this
    }

    private fun onRaised(command: Raised): Behavior<Command> {
        context.log.warn("ALARM {}", command.alarm.summary)
        return this
    }

    companion object {
        fun create(): Behavior<Command> = Behaviors.setup { context -> AlarmReporter(context) }
    }
}
