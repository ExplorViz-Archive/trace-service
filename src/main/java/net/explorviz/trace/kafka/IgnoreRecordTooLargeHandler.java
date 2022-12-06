package net.explorviz.trace.kafka;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;

/**
 * {@link ProductionExceptionHandler} used in the application.properties file. Currently, the
 * windowing may result in too big records ({@link RecordTooLargeException}) and the resulting
 * unhandled exception will result in an ERROR state, i.e., complete shutdown of the application.
 * Therefore, we catch the exception, discard the record, and proceed.
 */
public class IgnoreRecordTooLargeHandler implements ProductionExceptionHandler {

  // TODO how to get MicroMeterRegistry when there is no DI context?

  @Override
  public void configure(final Map<String, ?> configs) {
    // nothing to do
  }

  @Override
  public ProductionExceptionHandlerResponse handle(final ProducerRecord<byte[], byte[]> record,
      final Exception exception) {
    if (exception instanceof RecordTooLargeException) {
      return ProductionExceptionHandlerResponse.CONTINUE;
    } else {
      return ProductionExceptionHandlerResponse.FAIL;
    }
  }
}
