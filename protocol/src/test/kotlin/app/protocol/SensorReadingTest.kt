package app.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SensorReadingTest {

    @Test
    fun `parses the documented syntax`() {
        assertEquals(SensorReading("t1", 30.0), SensorReading.parse("sensor_id=t1; value=30"))
    }

    @Test
    fun `tolerates whitespace and fractions`() {
        assertEquals(SensorReading("h1", 40.5), SensorReading.parse("  sensor_id = h1 ;  value = 40.5 \n"))
    }

    @Test
    fun `rejects malformed payloads`() {
        assertNull(SensorReading.parse("sensor_id=t1"))
        assertNull(SensorReading.parse("value=30"))
        assertNull(SensorReading.parse("sensor_id=t1; value=hot"))
        assertNull(SensorReading.parse("sensor_id=t1; value=30; drop table sensors"))
    }
}
