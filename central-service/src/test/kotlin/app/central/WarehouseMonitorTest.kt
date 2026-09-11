package app.central

import app.central.actor.AlarmReporter
import app.central.actor.WarehouseMonitor
import app.central.alarm.Thresholds
import app.protocol.Measurement
import app.protocol.SensorKind
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit

class WarehouseMonitorTest {

    private val testKit = ActorTestKit.create()
    private val thresholds = Thresholds(mapOf(SensorKind.TEMPERATURE to 35.0, SensorKind.HUMIDITY to 50.0))

    @AfterTest
    fun shutdown() = testKit.shutdownTestKit()

    @Test
    fun `raises an alarm once per breach and clears it on recovery`() {
        val reporter = testKit.createTestProbe<AlarmReporter.Command>()
        val monitor = testKit.spawn(WarehouseMonitor.create("w1", thresholds, reporter.ref))

        monitor.tell(WarehouseMonitor.Observe(measurement(41.0)))
        assertEquals(41.0, reporter.expectMessageClass(AlarmReporter.Raised::class.java).alarm.value)

        monitor.tell(WarehouseMonitor.Observe(measurement(42.0)))
        reporter.expectNoMessage()

        monitor.tell(WarehouseMonitor.Observe(measurement(20.0)))
        assertEquals(20.0, reporter.expectMessageClass(AlarmReporter.Cleared::class.java).alarm.value)
    }

    @Test
    fun `stays silent below the threshold`() {
        val reporter = testKit.createTestProbe<AlarmReporter.Command>()
        val monitor = testKit.spawn(WarehouseMonitor.create("w1", thresholds, reporter.ref))

        monitor.tell(WarehouseMonitor.Observe(measurement(35.0)))

        reporter.expectNoMessage()
    }

    private fun measurement(value: Double) =
        Measurement("w1", "t1", SensorKind.TEMPERATURE, value, Clock.System.now())
}
