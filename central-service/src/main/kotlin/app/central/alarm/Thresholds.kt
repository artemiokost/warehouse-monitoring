package app.central.alarm

import app.protocol.SensorKind
import com.typesafe.config.Config

/** One limit per sensor kind; [from] refuses to start with any kind missing, so lookups never fail. */
class Thresholds(private val limits: Map<SensorKind, Double>) {

    init {
        require(limits.keys.containsAll(SensorKind.entries)) { "thresholds missing for ${SensorKind.entries - limits.keys}" }
    }

    fun limitOf(kind: SensorKind): Double = limits.getValue(kind)

    companion object {
        fun from(config: Config): Thresholds =
            Thresholds(SensorKind.entries.associateWith { kind -> config.getDouble(kind.name) })
    }
}
