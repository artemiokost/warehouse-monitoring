package app.warehouse.settings

import app.common.config.getNonBlankString
import app.common.protocol.SensorType
import com.typesafe.config.Config

class WarehouseSettings(config: Config) {

    val id: String = config.getNonBlankString("warehouse.id")
    val measurementSubject: String = config.getNonBlankString("nats.measurement-subject")

    val sensors: List<SensorBinding> = config.getConfigList("warehouse.sensors").map { sensor ->
        SensorBinding(
            type = SensorType.valueOf(sensor.getString("type")),
            host = sensor.getString("host"),
            port = sensor.getInt("port"),
        )
    }

    init {
        require(id.none { it.isWhitespace() || it in ".*>" }) {
            "warehouse.id '$id' is not a single NATS subject token"
        }
    }
}
