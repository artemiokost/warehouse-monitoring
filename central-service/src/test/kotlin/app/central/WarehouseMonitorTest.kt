package app.central

import app.central.actor.AlarmReporter
import app.central.actor.WarehouseMonitor
import app.central.actor.WarehouseRegistry
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
    private val reporter = testKit.createTestProbe<AlarmReporter.Command>()
    private val ingress = testKit.createTestProbe<WarehouseRegistry.Ack>()

    @AfterTest
    fun shutdown() = testKit.shutdownTestKit()

    @Test
    fun `raises an alarm once per breach and clears it on recovery`() {
        val monitor = testKit.spawn(WarehouseMonitor.create("w1", thresholds, reporter.ref))

        monitor.tell(WarehouseMonitor.Observe(measurement(41.0), ingress.ref))
        assertEquals(41.0, reporter.expectMessageClass(AlarmReporter.Raised::class.java).alarm.value)

        monitor.tell(WarehouseMonitor.Observe(measurement(42.0), ingress.ref))
        reporter.expectNoMessage()

        monitor.tell(WarehouseMonitor.Observe(measurement(20.0), ingress.ref))
        assertEquals(20.0, reporter.expectMessageClass(AlarmReporter.Cleared::class.java).alarm.value)
    }

    @Test
    fun `stays silent at the threshold`() {
        val monitor = testKit.spawn(WarehouseMonitor.create("w1", thresholds, reporter.ref))

        monitor.tell(WarehouseMonitor.Observe(measurement(35.0), ingress.ref))

        reporter.expectNoMessage()
    }

    @Test
    fun `acknowledges every measurement so the ingress can send the next one`() {
        val monitor = testKit.spawn(WarehouseMonitor.create("w1", thresholds, reporter.ref))

        monitor.tell(WarehouseMonitor.Observe(measurement(41.0), ingress.ref))
        monitor.tell(WarehouseMonitor.Observe(measurement(42.0), ingress.ref))

        ingress.expectMessage(WarehouseRegistry.Ack)
        ingress.expectMessage(WarehouseRegistry.Ack)
    }

    private fun measurement(value: Double) =
        Measurement("w1", "t1", SensorKind.TEMPERATURE, value, Clock.System.now())
}
