package net.explorviz.trace.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import javax.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

/**
 * Registers a {@link StreamsUncaughtExceptionHandler} for the {@link KafkaStreams}. Currently, the
 * windowing may result in too big records and the resulting unhandled exception will result in an
 * ERROR state, i.e., complete shutdown of the application. Therefore, we catch the exception,
 * discard the record, and proceed.
 */
public class IgnoreRecordTooLargeHandler implements ProductionExceptionHandler {

  private static final String METRIC_NAME_TOO_LARGE_RECORDS = "explorviz_total_too_large_records";

  @Inject
  /* default */ MeterRegistry registry; // NOCS

  private Counter counterTooLargeRecords;

  @Override
  public void configure(final Map<String, ?> configs) {
    this.counterTooLargeRecords = this.registry.counter(METRIC_NAME_TOO_LARGE_RECORDS);
  }

  @Override
  public ProductionExceptionHandlerResponse handle(final ProducerRecord<byte[], byte[]> record,
      final Exception exception) {
    if (exception instanceof RecordTooLargeException) {
      this.counterTooLargeRecords.increment();
      return ProductionExceptionHandlerResponse.CONTINUE;
    } else {
      return ProductionExceptionHandlerResponse.FAIL;
    }
  }
}
