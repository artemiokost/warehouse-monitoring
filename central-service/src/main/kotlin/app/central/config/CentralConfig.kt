package app.central.config

import app.central.alarm.Thresholds
import com.typesafe.config.Config

data class CentralConfig(
    val natsUrl: String,
    val thresholds: Thresholds,
) {

    companion object {
        fun from(config: Config): CentralConfig = config.getConfig("central").let { section ->
            CentralConfig(
                natsUrl = section.getString("nats-url"),
                thresholds = Thresholds.from(section.getConfig("thresholds")),
            )
        }
    }
}
