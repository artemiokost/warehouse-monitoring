package app.protocol

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Measurement(
    val warehouseId: String,
    val sensorId: String,
    val kind: SensorKind,
    val value: Double,
    val at: Instant,
) {
    init {
        Subjects.checkWarehouseId(warehouseId)
    }
}
