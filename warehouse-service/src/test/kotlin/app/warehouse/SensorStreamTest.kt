package app.warehouse

import app.common.protocol.Measurement
import app.common.protocol.Sensor
import app.common.protocol.SensorType.HUMIDITY
import app.warehouse.settings.SensorBinding
import app.warehouse.stream.SensorStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit

class SensorStreamTest {

    private val testKit = ActorTestKit.create()
    private val received = testKit.createTestProbe<Measurement>()

    @AfterTest
    fun shutdown() = testKit.shutdownTestKit()

    @Test
    fun `datagram becomes a measurement of the bound type`() {
        val port = DatagramSocket(0).use { it.localPort }
        val binding = SensorBinding(HUMIDITY, "127.0.0.1", port)
        SensorStream
            .source(binding, "w1", testKit.system())
            .runForeach(
                { received.ref.tell(it) },
                testKit.system()
            )

        val bytes = "sensor_id=h1; value=73.5".encodeToByteArray()
        val packet = DatagramPacket(bytes, bytes.size, InetSocketAddress("127.0.0.1", port))
        val measurement = DatagramSocket().use { socket ->
            received.awaitAssert {
                socket.send(packet)
                received.receiveMessage(Duration.ofMillis(200))
            }
        }

        val expected = Measurement(Sensor("w1", "h1", HUMIDITY), 73.5, measurement.measuredAt)
        assertEquals(expected, measurement)
    }
}
