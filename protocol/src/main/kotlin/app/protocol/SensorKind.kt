package app.protocol

enum class SensorKind(val unit: String) {
    HUMIDITY("%"),
    TEMPERATURE("°C"),
}
