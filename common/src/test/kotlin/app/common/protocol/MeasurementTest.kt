package app.common.protocol

import app.common.protocol.SensorType.HUMIDITY
import app.common.protocol.SensorType.TEMPERATURE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

class MeasurementTest {

    private val now = Clock.System.now()

    @Test
    fun `valid syntax`() {
        assertEquals(
            Measurement(Sensor("w1", "t1", TEMPERATURE), 30.0, now),
            Measurement.parse("sensor_id=t1; value=30", "w1", TEMPERATURE, now),
        )
        assertEquals(
            Measurement(Sensor("w1", "h1", HUMIDITY), 40.5, now),
            Measurement.parse("sensor_id=h1; value=40.5", "w1", HUMIDITY, now),
        )
    }

    @Test
    fun `invalid syntax`() {
        listOf(
            "sensor_id=t1",
            "value=30",
            "sensor_id=t1; value=",
            "sensor_id=t1; value=hot",
            "sensor_id=t1; value=30; extra",
            "sensor_id=t1; value=NaN",
            "sensor_id=t1; value=Infinity",
        ).forEach { assertNull(Measurement.parse(it, "w1", TEMPERATURE, now), it) }
    }
}
