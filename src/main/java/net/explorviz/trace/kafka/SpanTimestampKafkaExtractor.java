package net.explorviz.trace.kafka;

import io.quarkus.runtime.annotations.RegisterForReflection;
import net.explorviz.avro.Span;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/**
 * Timestamp extractor for spans. Uses the start time of a spans as the record's timestamp used for
 * windowing.
 */
@RegisterForReflection
public class SpanTimestampKafkaExtractor implements TimestampExtractor {

  public SpanTimestampKafkaExtractor() {// NOPMD
    // nothing to do, necessary for native image
  }

  @Override
  public long extract(final ConsumerRecord<Object, Object> record, final long previousTimestamp) {
    final Span span = (Span) record.value();

    if (span != null) {
      // timestamp = Duration.ofNanos(span.getStartTime()).toMillis();
      // timestamp = Instant.ofEpochMilli(span.getStartTime()).toEpochMilli();
      return span.getStartTimeEpochMilli();
    }

    // Invalid timestamp! Attempt to estimate a new timestamp,
    // otherwise fall back to wall-clock time (processing-time).
    if (previousTimestamp >= 0) {
      return previousTimestamp;
    } else {
      return System.currentTimeMillis();
    }


  }


}
