package app.warehouse

import app.warehouse.actor.WarehouseGuardian
import org.apache.pekko.actor.typed.ActorSystem

fun main() {
    ActorSystem.create(WarehouseGuardian.create(), "warehouse")
}
