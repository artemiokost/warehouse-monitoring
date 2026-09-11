package app.warehouse.settings

import app.common.protocol.SensorType

data class SensorBinding(val type: SensorType, val host: String, val port: Int)
