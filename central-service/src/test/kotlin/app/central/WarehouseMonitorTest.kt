package app.central

import app.central.actor.AlarmReporter
import app.central.actor.AlarmReporter.Cleared
import app.central.actor.AlarmReporter.Raised
import app.central.actor.WarehouseMonitor
import app.central.actor.WarehouseMonitor.Observe
import app.common.protocol.Measurement
import app.common.protocol.Sensor
import app.common.protocol.SensorType
import app.common.protocol.SensorType.HUMIDITY
import app.common.protocol.SensorType.TEMPERATURE
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import org.apache.pekko.Done
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit

class WarehouseMonitorTest {

    private val testKit = ActorTestKit.create()
    private val reporter = testKit.createTestProbe<AlarmReporter.Command>()
    private val stream = testKit.createTestProbe<Done>()
    private val thresholds = mapOf(TEMPERATURE to 35.0, HUMIDITY to 50.0)
    private val monitor = testKit.spawn(WarehouseMonitor.create(thresholds, reporter.ref))

    @AfterTest
    fun shutdown() = testKit.shutdownTestKit()

    @Test
    fun `alarm once per crossing`() {
        send(41.0)
        reporter.expectMessageClass(Raised::class.java)

        send(42.0)
        reporter.expectNoMessage()

        send(20.0)
        reporter.expectMessageClass(Cleared::class.java)
    }

    @Test
    fun `threshold itself is not a breach`() {
        send(35.0)
        reporter.expectNoMessage()
    }

    @Test
    fun `same id, different type, separate state`() {
        send(41.0)
        reporter.expectMessageClass(Raised::class.java)

        send(40.0, HUMIDITY)
        reporter.expectNoMessage()
    }

    @Test
    fun `same id, different warehouse, separate state`() {
        send(41.0)
        send(41.0, warehouseId = "w2")

        val raised = List(2) { reporter.expectMessageClass(Raised::class.java).measurement.sensor.warehouseId }
        assertEquals(setOf("w1", "w2"), raised.toSet())
    }

    private fun send(value: Double, type: SensorType = TEMPERATURE, warehouseId: String = "w1") {
        val measurement = Measurement(Sensor(warehouseId, "s1", type), value, Clock.System.now())
        monitor.tell(Observe(measurement, stream.ref))
    }
}
