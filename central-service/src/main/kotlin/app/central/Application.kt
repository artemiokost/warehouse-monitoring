package app.central

import app.central.actor.CentralGuardian
import org.apache.pekko.actor.typed.ActorSystem

fun main() {
    ActorSystem.create(CentralGuardian.create(), "central")
}
