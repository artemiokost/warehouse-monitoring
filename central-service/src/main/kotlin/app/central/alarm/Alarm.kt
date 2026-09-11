package app.central.alarm

import app.protocol.SensorKind
import kotlin.time.Instant

data class Alarm(
    val warehouseId: String,
    val sensorId: String,
    val kind: SensorKind,
    val value: Double,
    val threshold: Double,
    val at: Instant,
) {
    val summary: String
        get() = "warehouse=$warehouseId sensor=$sensorId $kind=$value${kind.unit} threshold=$threshold${kind.unit} at=$at"
}
