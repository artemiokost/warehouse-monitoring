package app.central.alarm

import app.protocol.SensorKind

data class Alarm(
    val warehouseId: String,
    val sensorId: String,
    val kind: SensorKind,
    val value: Double,
    val threshold: Double,
) {
    val summary: String
        get() = "warehouse=$warehouseId sensor=$sensorId $kind=$value${kind.unit} threshold=$threshold${kind.unit}"
}
