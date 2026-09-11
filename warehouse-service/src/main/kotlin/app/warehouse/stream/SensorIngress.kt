package app.warehouse.stream

import app.protocol.Measurement
import app.protocol.SensorReading
import app.warehouse.actor.MeasurementPublisher
import app.warehouse.config.SensorBinding
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.time.Clock
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.RestartSettings
import org.apache.pekko.stream.connectors.udp.Datagram
import org.apache.pekko.stream.connectors.udp.javadsl.Udp
import org.apache.pekko.stream.javadsl.Keep
import org.apache.pekko.stream.javadsl.RestartSource
import org.apache.pekko.stream.javadsl.Source
import org.apache.pekko.stream.typed.javadsl.ActorSink
import org.slf4j.Logger

object SensorIngress {

    private val MAX_BACKOFF = Duration.ofSeconds(30)
    private val MIN_BACKOFF = Duration.ofSeconds(1)
    private const val RANDOM_FACTOR = 0.2

    fun start(
        binding: SensorBinding,
        warehouseId: String,
        publisher: ActorRef<MeasurementPublisher.Command>,
        system: ActorSystem<*>,
    ) {
        val address = InetSocketAddress(binding.host, binding.port)
        val log = system.log()

        RestartSource
            .onFailuresWithBackoff(RestartSettings.create(MIN_BACKOFF, MAX_BACKOFF, RANDOM_FACTOR)) {
                Source.maybe<Datagram>()
                    .viaMat(Udp.bindFlow(address, system), Keep.right())
                    .mapMaterializedValue { bound ->
                        bound.thenAccept { log.info("Listening for {} datagrams on {}", binding.kind, it) }
                    }
            }
            .mapConcat { datagram -> listOfNotNull(read(datagram, log)) }
            .map { reading ->
                Measurement(warehouseId, reading.sensorId, binding.kind, reading.value, Clock.System.now())
            }
            .to(
                ActorSink.actorRefWithBackpressure(
                    publisher,
                    { ackTo, measurement -> MeasurementPublisher.Publish(measurement, ackTo) },
                    { ackTo -> MeasurementPublisher.IngressStarted(ackTo) },
                    MeasurementPublisher.Ack,
                    MeasurementPublisher.IngressStopped(null),
                    { cause -> MeasurementPublisher.IngressStopped(cause) },
                )
            )
            .run(system)
    }

    private fun read(datagram: Datagram, log: Logger): SensorReading? {
        val raw = datagram.data.utf8String()
        val reading = SensorReading.parse(raw)

        if (reading == null) {
            log.warn("Discarded malformed datagram from {}: {}", datagram.remote, raw)
        }

        return reading
    }
}
