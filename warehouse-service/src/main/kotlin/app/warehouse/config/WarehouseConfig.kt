package app.warehouse.config

import app.protocol.SensorKind
import com.typesafe.config.Config

data class SensorBinding(val kind: SensorKind, val host: String, val port: Int)

data class WarehouseConfig(
    val id: String,
    val natsUrl: String,
    val sensors: List<SensorBinding>,
) {

    companion object {
        fun from(config: Config): WarehouseConfig = config.getConfig("warehouse").let { section ->
            WarehouseConfig(
                id = section.getString("id"),
                natsUrl = section.getString("nats-url"),
                sensors = section.getConfigList("sensors").map { sensor ->
                    SensorBinding(
                        kind = SensorKind.valueOf(sensor.getString("kind")),
                        host = sensor.getString("host"),
                        port = sensor.getInt("port"),
                    )
                },
            )
        }
    }
}
