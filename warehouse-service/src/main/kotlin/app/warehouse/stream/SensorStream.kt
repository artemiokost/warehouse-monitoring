package app.warehouse.stream

import app.common.protocol.Measurement
import app.common.protocol.SensorType
import app.warehouse.settings.SensorBinding
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.RestartSettings
import org.apache.pekko.stream.connectors.udp.Datagram
import org.apache.pekko.stream.connectors.udp.javadsl.Udp
import org.apache.pekko.stream.javadsl.Keep
import org.apache.pekko.stream.javadsl.RestartSource
import org.apache.pekko.stream.javadsl.Source
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.time.Clock

object SensorStream {

    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    private val REBIND = RestartSettings.create(Duration.ofSeconds(1), Duration.ofSeconds(30), 0.2)

    fun source(
        binding: SensorBinding,
        warehouseId: String,
        system: ActorSystem<*>
    ): Source<Measurement, NotUsed> = RestartSource
        .onFailuresWithBackoff(REBIND) { datagramsFrom(binding, system) }
        .mapConcat { datagram -> listOfNotNull(read(datagram, warehouseId, binding.type)) }

    private fun datagramsFrom(
        binding: SensorBinding,
        system: ActorSystem<*>
    ) = Source.never<Datagram>()
        .viaMat(Udp.bindFlow(InetSocketAddress(binding.host, binding.port), system), Keep.right())
        .mapMaterializedValue { bound ->
            bound.thenAccept { logger.info("Listening for {} datagrams on {}", binding.type, it) }
        }

    private fun read(datagram: Datagram, warehouseId: String, type: SensorType): Measurement? {
        val raw = datagram.data.utf8String()
        val measurement = Measurement.parse(raw, warehouseId, type, Clock.System.now())

        if (measurement == null) {
            logger.warn("Discarded malformed datagram from {}: {}", datagram.remote, raw)
        }

        return measurement
    }
}
