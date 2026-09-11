package app.protocol

data class SensorReading(val sensorId: String, val value: Double) {

    companion object {
        private val SYNTAX = Regex("""sensor_id\s*=\s*([\w-]+)\s*;\s*value\s*=\s*(-?\d+(?:[.,]\d+)?)""")

        fun parse(raw: String): SensorReading? = SYNTAX.matchEntire(raw.trim())?.let { match ->
            SensorReading(match.groupValues[1], match.groupValues[2].replace(',', '.').toDouble())
        }
    }
}
