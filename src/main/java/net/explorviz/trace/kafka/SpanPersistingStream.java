package net.explorviz.trace.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.explorviz.avro.SpanDynamic;
import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceAggregator;
import net.explorviz.trace.service.TraceRepository;
import net.explorviz.trace.service.reduction.CallTree;
import net.explorviz.trace.service.reduction.CallTreeConverter;
import net.explorviz.trace.service.reduction.DepthReducer;
import net.explorviz.trace.service.reduction.SimpleLoopReducer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.State;
import org.apache.kafka.streams.KafkaStreams.StateListener;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains a Kafka Streams topology that ingests {@link SpanDynamic} from a topic and aggregates
 * spans that belong to a the same trace in a {@link Trace} object. The later contains a list of
 * said spans as well as meta information, e.g., the landscape token. The resulting traces are
 * persisted in a cassandra db.
 */
@ApplicationScoped
public class SpanPersistingStream {

  public static final long WINDOW_SIZE_MS = 10_000;
  public static final long GRACE_MS = 2_000;
  public static final int REDUCTION_DEPTH = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger(SpanPersistingStream.class);

  @Inject
  // NOPMD
  /* default */ MeterRegistry meterRegistry; // NOPMD NOCS

  private final AtomicInteger lastReceivedTotalSpans = new AtomicInteger(0);
  private final AtomicInteger reconstructedTracesCount = new AtomicInteger(0);
  private final AtomicInteger spanReducedTracesCount = new AtomicInteger(0);

  private final Properties streamsConfig = new Properties();
  private final Topology topology;

  private final SchemaRegistryClient registryClient;
  private final KafkaConfig config;

  private KafkaStreams streams;

  private final TraceRepository traceRepository;

  // Reducer
  private final DepthReducer depthReducer;
  private final SimpleLoopReducer loopReducer;

  @Inject
  public SpanPersistingStream(final SchemaRegistryClient schemaRegistryClient,
      final KafkaConfig config, final TraceRepository traceRepository) {

    this.registryClient = schemaRegistryClient;
    this.config = config;

    this.topology = this.buildTopology();
    this.setupStreamsConfig();

    this.traceRepository = traceRepository;

    this.depthReducer = new DepthReducer(REDUCTION_DEPTH);
    this.loopReducer = new SimpleLoopReducer();
  }

  /* default */ void onStart(@Observes final StartupEvent event) { // NOPMD
    this.streams = new KafkaStreams(this.topology, this.streamsConfig);
    this.streams.cleanUp();
    this.streams.setStateListener(new ErrorStateListener());

    this.streams.start();

    final KafkaStreamsMetrics ksm = new KafkaStreamsMetrics(this.streams);// NOPMD
    ksm.bindTo(this.meterRegistry);
  }

  /* default */ void onStop(@Observes final ShutdownEvent event) { // NOPMD
    this.streams.close();
  }

  private void setupStreamsConfig() {
    this.streamsConfig.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
        this.config.getBootstrapServers());
    this.streamsConfig.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
        this.config.getCommitIntervalMs());
    this.streamsConfig.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
        this.config.getTimestampExtractor());
    this.streamsConfig.put(StreamsConfig.APPLICATION_ID_CONFIG, this.config.getApplicationId());

    // enable producing of bigger records
    this.streamsConfig.put("max.request.size", this.config.getMaxRecordSize());

    // enable consuming of bigger records
    this.streamsConfig.put("max.partition.fetch.bytes", this.config.getMaxRecordSize());
  }

  private Topology buildTopology() {

    // TODO Reduction of multiple traces to a single representative?

    final StreamsBuilder builder = new StreamsBuilder();

    final KStream<String, SpanDynamic> spanStream = builder.stream(this.config.getInTopic(),
        Consumed.with(Serdes.String(), this.getAvroSerde(false)));

    // DEBUG Total spans
    spanStream.foreach((key, value) -> {
      this.lastReceivedTotalSpans.incrementAndGet();
    });

    final TimeWindows traceWindow =
        TimeWindows.of(Duration.ofMillis(WINDOW_SIZE_MS)).grace(Duration.ofMillis(GRACE_MS));

    final TraceAggregator aggregator = new TraceAggregator();

    // Group by landscapeToken::TraceId
    final KTable<Windowed<String>, Trace> traceTable = spanStream
        .groupBy((k, v) -> v.getLandscapeToken() + "::" + v.getTraceId(),
            Grouped.with(Serdes.String(), this.getAvroSerde(false)))
        .windowedBy(traceWindow)
        .aggregate(Trace::new, (key, value, aggregate) -> aggregator.aggregate(aggregate, value),
            Materialized.with(Serdes.String(), this.getAvroSerde(false)))
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
          LOGGER.trace("Reduced {} spans", tracesOriginal - reducedTrace.getSpanList().size());
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

      this.traceRepository.insert(t).await().indefinitely();
    });

    return builder.build();
  }

  @Scheduled(every = "{explorviz.log.span.interval}") // NOPMD
  void logStatus() { // NOPMD
    final int totalSpans = this.lastReceivedTotalSpans.getAndSet(0);
    final int reconstructedTraces = this.reconstructedTracesCount.getAndSet(0);
    final int spanReducedTraces = this.spanReducedTracesCount.getAndSet(0);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Received {} spans: {} trace reconstructed in"
              + " {} time window, the Spans of {} traces have been reduced.",
          totalSpans, reconstructedTraces, WINDOW_SIZE_MS, spanReducedTraces);
    }
  }

  /**
   * Creates a {@link Serde} for specific avro records using the {@link SpecificAvroSerde}.
   *
   * @param forKey {@code true} if the Serde is for keys, {@code false} otherwise
   * @param <T> type of the avro record
   * @return a Serde
   */
  private <T extends SpecificRecord> SpecificAvroSerde<T> getAvroSerde(final boolean forKey) {
    final SpecificAvroSerde<T> serde = new SpecificAvroSerde<>(this.registryClient);
    serde.configure(Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
        this.config.getSchemaRegistryUrl()), forKey);

    return serde;
  }

  private static class ErrorStateListener implements StateListener {

    @Override
    public void onChange(final State newState, final State oldState) {
      if (newState.equals(State.ERROR)) {

        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Kafka Streams thread died. "
              + "Are Kafka topic initialized? Quarkus application will shut down.");
        }
        Quarkus.asyncExit(-1);
      }

    }
  }



  public Topology getTopology() {
    return this.topology;
  }


}

