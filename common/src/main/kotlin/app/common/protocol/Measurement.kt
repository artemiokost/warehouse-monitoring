package app.common.protocol

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class Measurement(val sensor: Sensor, val value: Double, val measuredAt: Instant) {

    fun describe(thresholds: Map<SensorType, Double>): String {
        val threshold = thresholds.getValue(sensor.type)
        return listOf(
            "warehouse=${sensor.warehouseId}",
            "sensor=${sensor.id}",
            "${sensor.type}=$value${sensor.type.unit}",
            "threshold=$threshold${sensor.type.unit}",
            "measuredAt=$measuredAt",
        ).joinToString(" ")
    }

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }
        private val SYNTAX = Regex("""sensor_id\s*=\s*([\w-]+)\s*;\s*value\s*=\s*(\S+)""")

        fun parse(datagram: String, warehouseId: String, type: SensorType, measuredAt: Instant): Measurement? {
            val (id, value) = SYNTAX.matchEntire(datagram.trim())?.destructured ?: return null
            val number = value.toDoubleOrNull() ?: return null
            if (!number.isFinite()) return null
            return Measurement(Sensor(warehouseId, id, type), number, measuredAt)
        }

        fun encode(measurement: Measurement): ByteArray {
            return JSON.encodeToString(measurement).encodeToByteArray()
        }

        fun decode(payload: ByteArray): Measurement? =
            try {
                JSON.decodeFromString<Measurement>(payload.decodeToString())
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
    }
}
