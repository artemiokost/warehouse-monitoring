package app.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class MeasurementCodecTest {

    @Test
    fun `survives a round trip`() {
        val measurement = Measurement("w1", "t1", SensorKind.TEMPERATURE, 41.5, Instant.fromEpochMilliseconds(1_700_000_000_000))

        assertEquals(measurement, MeasurementCodec.decode(MeasurementCodec.encode(measurement)))
    }

    @Test
    fun `rejects foreign payloads`() {
        assertNull(MeasurementCodec.decode("not json".encodeToByteArray()))
    }
}
