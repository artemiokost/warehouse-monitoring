package app.warehouse.stream

import app.common.protocol.Measurement
import io.nats.client.Connection
import org.apache.pekko.Done
import org.apache.pekko.stream.javadsl.Sink
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletionStage

object MeasurementPublisher {

    private val logger = LoggerFactory.getLogger(this::class.simpleName)

    fun sink(
        connection: Connection,
        subject: String
    ): Sink<Measurement, CompletionStage<Done>> = Sink.foreach { measurement ->
        try {
            connection.publish(subject, Measurement.encode(measurement))
        } catch (e: RuntimeException) {
            logger.warn("Dropped measurement from {}: {}", measurement.sensor.id, e.toString())
        }
    }
}
