package app.central.settings

import app.common.config.getNonBlankString
import app.common.protocol.SensorType
import com.typesafe.config.Config

class CentralSettings(config: Config) {

    val measurementSubject: String = config.getNonBlankString("nats.measurement-subject")

    val thresholds: Map<SensorType, Double> = SensorType.entries.associateWith { type ->
        config.getDouble("central.thresholds.${type.name}")
    }
}
