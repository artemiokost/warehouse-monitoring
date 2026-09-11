package app.protocol

import kotlinx.serialization.json.Json

object MeasurementCodec {

    fun decode(payload: ByteArray): Measurement? =
        runCatching { Json.decodeFromString<Measurement>(payload.decodeToString()) }.getOrNull()

    fun encode(measurement: Measurement): ByteArray =
        Json.encodeToString(measurement).encodeToByteArray()
}
