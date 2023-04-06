package net.explorviz.trace.kafka;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.scheduler.Scheduled;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;
import net.explorviz.trace.persistence.ReactiveTraceService;
import net.explorviz.trace.service.HashHelper;
import net.explorviz.trace.service.TraceAggregator;
import net.explorviz.trace.service.TraceConverter;
import net.explorviz.trace.service.reduction.CallTree;
import net.explorviz.trace.service.reduction.CallTreeConverter;
import net.explorviz.trace.service.reduction.DepthReducer;
import net.explorviz.trace.service.reduction.SimpleLoopReducer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a KafkaStream topology instance with all its transformers. Entry point of the stream
 * analysis.
 */
@ApplicationScoped
public class TopologyProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyProducer.class);

  @ConfigProperty(name = "explorviz.kafka-streams.topics.in")
  /* default */ String inTopic;

  @ConfigProperty(name = "explorviz.kafka-streams.window.size")
  /* default */ long windowSizeInMs;

  @ConfigProperty(name = "explorviz.kafka-streams.window.grace")
  /* default */ long graceSizeInMs;

  @ConfigProperty(name = "explorviz.kafka-streams.discard")
  /* default */ boolean discard;

  @Inject
  /* default */ SpecificAvroSerde<Span> dynamicAvroSerde;

  @Inject
  /* default */ SpecificAvroSerde<Trace> traceAvroSerde;

  @Inject
  /* default */ ReactiveTraceService reactiveTraceService;

  @Inject
  /* default */ DepthReducer depthReducer;
  @Inject
  /* default */ SimpleLoopReducer loopReducer;


  // Logged and reset every n seconds
  private final AtomicInteger lastReceivedTotalSpans = new AtomicInteger(0);
  private final AtomicInteger reconstructedTracesCount = new AtomicInteger(0);
  private final AtomicInteger spanReducedTracesCount = new AtomicInteger(0);

  /**
   * Builds a Kafka Streams topology to process and aggregate spans into traces, and returns the
   * constructed {@link Topology}.
   *
   * @return the constructed Kafka Streams {@link Topology}.
   */
  @Produces
  public Topology buildTopology() {

    final StreamsBuilder builder = new StreamsBuilder();

    // BEGIN Span conversion

    final KStream<String, Span> spanStream =
        builder.stream(this.inTopic, Consumed.with(Serdes.String(), this.dynamicAvroSerde));

    final KStream<String, Span> spanStreamWithHashCodes =
        spanStream.mapValues((readOnlyKey, value) -> {
          value.setHashCode(HashHelper.createHash(value));
          return value;
        });

    // DEBUG Total spans
    spanStreamWithHashCodes.foreach((key, value) -> {
      this.lastReceivedTotalSpans.incrementAndGet();
    });

    if (this.discard) {
      return builder.build();
    }

    final TimeWindows traceWindow =
        TimeWindows.ofSizeAndGrace(Duration.ofMillis(this.windowSizeInMs),
            Duration.ofMillis(this.graceSizeInMs));

    final TraceAggregator aggregator = new TraceAggregator();

    // Group by landscapeToken::TraceId
    final KTable<Windowed<String>, Trace> traceTable =
        spanStreamWithHashCodes.groupBy((k, v) -> v.getLandscapeToken() + "::" + v.getTraceId(),
                Grouped.with(Serdes.String(), this.dynamicAvroSerde)).windowedBy(traceWindow)
            .aggregate(Trace::new,
                (key, value, aggregate) -> aggregator.aggregate(aggregate, value),
                Materialized.with(Serdes.String(), this.traceAvroSerde))
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));

    final KStream<String, Trace> traceStream =
        traceTable.toStream().selectKey((k, v) -> v.getLandscapeToken() + "::" + k);

    // DEBUG Total traces for window
    traceStream.foreach((key, value) -> {
      this.reconstructedTracesCount.incrementAndGet();
    });

    // traceStream.foreach(
    // (key, value) -> System.out.println("|Trace.spans()| = " + value.getSpanList().size()));

    final KStream<String, Trace> reducedTraceStream = traceStream.mapValues((k, trace) -> {
      final int tracesOriginal = trace.getSpanList().size();
      try {
        final CallTree tree = CallTreeConverter.toTree(trace);
        CallTree reduced = this.depthReducer.reduce(tree);
        reduced = this.loopReducer.reduce(reduced);
        final Trace reducedTrace = CallTreeConverter.toTrace(reduced);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Reduced trace with {} original spans to {} spans.", tracesOriginal,
              reducedTrace.getSpanList().size());
        }
        return reducedTrace;
      } catch (final IllegalArgumentException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Could not perform reduction: {}", e.getMessage());
        }
        return trace;
      }

    });

    // reducedTraceStream.foreach((key, value) -> System.out
    // .println("Reduction |Trace.spans()| = " + value.getSpanList().size()));

    reducedTraceStream.foreach((k, t) -> {

      // DEBUG Total traces for window
      this.spanReducedTracesCount.incrementAndGet();

      this.reactiveTraceService.insert(TraceConverter.convertTraceToDao(t)).subscribe()
          .with(unused -> {
          }, failure -> {
            if (LOGGER.isErrorEnabled()) {
              LOGGER.error("Could not persist trace", failure);
            }
          });
    });

    // END Span conversion

    return builder.build();
  }

  @Scheduled(every = "{explorviz.log.span.interval}")
    /* default */ void logStatus() {
    final int totalSpans = this.lastReceivedTotalSpans.getAndSet(0);
    final int reconstructedTraces = this.reconstructedTracesCount.getAndSet(0);
    final int spanReducedTraces = this.spanReducedTracesCount.getAndSet(0);
    if (LOGGER.isDebugEnabled()) {
      if (this.discard) {
        LOGGER.debug("Received and discarded {} spans.", totalSpans);
      } else {
        LOGGER.debug("Received {} spans: {} trace reconstructed in"
                + " {} time window, the Spans of {} traces have been reduced.", totalSpans,
            reconstructedTraces, this.windowSizeInMs, spanReducedTraces);
      }
    }
  }

}
