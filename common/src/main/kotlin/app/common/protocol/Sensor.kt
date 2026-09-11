package app.common.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Sensor(val warehouseId: String, val id: String, val type: SensorType)
