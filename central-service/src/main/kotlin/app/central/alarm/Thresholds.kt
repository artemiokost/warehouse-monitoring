package app.central.alarm

import app.protocol.SensorKind
import com.typesafe.config.Config

class Thresholds(private val limits: Map<SensorKind, Double>) {

    fun limitOf(kind: SensorKind): Double? = limits[kind]

    companion object {
        fun from(config: Config): Thresholds =
            Thresholds(SensorKind.entries.associateWith { kind -> config.getDouble(kind.name) })
    }
}
