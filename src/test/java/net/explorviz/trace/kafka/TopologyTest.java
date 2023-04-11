package net.explorviz.trace.kafka;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import net.explorviz.avro.Span;
import net.explorviz.trace.helper.TraceHelper;
import net.explorviz.trace.persistence.ReactiveTraceService;
import net.explorviz.trace.persistence.dao.Trace;
import net.explorviz.trace.service.HashHelper;
import net.explorviz.trace.service.TimestampHelper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
class TopologyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyTest.class);
  @ConfigProperty(name = "explorviz.kafka-streams.topics.in")
  /* default */ String inTopic;
  @ConfigProperty(name = "explorviz.kafka-streams.window.size")
  /* default */ long windowSizeInMs;
  @ConfigProperty(name = "explorviz.kafka-streams.window.grace")
  /* default */ long graceSizeInMs;
  @Inject
  Topology topology;
  @Inject
  SpecificAvroSerde<Span> spanSerde;
  ReactiveTraceService reactiveTraceService;
  private TopologyTestDriver testDriver;
  private TestInputTopic<String, Span> inputTopic;

  @BeforeEach
  void setUp() {
    final Properties config = new Properties();
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName());
    config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://registry:1234");
    config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
        SpanTimestampKafkaExtractor.class);

    this.testDriver = new TopologyTestDriver(this.topology, config);

    this.reactiveTraceService = Mockito.mock(ReactiveTraceService.class);
    QuarkusMock.installMockForType(this.reactiveTraceService, ReactiveTraceService.class);

    this.inputTopic = this.testDriver.createInputTopic(this.inTopic, Serdes.String().serializer(),
        this.spanSerde.serializer());

  }

  @AfterEach
  void afterEach() {
    this.spanSerde.close();
    this.testDriver.getAllStateStores().forEach((k, v) -> v.close());
    this.testDriver.close();
  }

  /**
   * Check if a single span has been saved in a trace.
   */
  @Test
  void testSingleSpan() {

    final List<Trace> mockSpanDB = new ArrayList<>();

    Mockito.doAnswer(i -> {
          final Trace inserted = i.getArgument(0, Trace.class);
          mockSpanDB.add(inserted);
          return Uni.createFrom().nullItem();
        }).when(this.reactiveTraceService)
        .insert(ArgumentMatchers.any(net.explorviz.trace.persistence.dao.Trace.class));

    final Span testSpan = TraceHelper.randomSpan();

    this.inputTopic.pipeInput(testSpan.getTraceId(), testSpan);
    this.forceSuppression(testSpan.getStartTimeEpochMilli());

    Assertions.assertEquals(1, mockSpanDB.size());

    Assertions.assertEquals(testSpan.getLandscapeToken(),
        mockSpanDB.get(0).getSpanList().get(0).getLandscapeToken());

    Assertions.assertEquals(testSpan.getTraceId(),
        mockSpanDB.get(0).getSpanList().get(0).getTraceId());

    Assertions.assertEquals(testSpan.getSpanId(),
        mockSpanDB.get(0).getSpanList().get(0).getSpanId());

    Assertions.assertEquals(testSpan.getParentSpanId(),
        mockSpanDB.get(0).getSpanList().get(0).getParentSpanId());

    Assertions.assertEquals(testSpan.getStartTimeEpochMilli(),
        mockSpanDB.get(0).getSpanList().get(0).getStartTime());

    Assertions.assertEquals(testSpan.getEndTimeEpochMilli(),
        mockSpanDB.get(0).getSpanList().get(0).getEndTime());

    Assertions.assertEquals(HashHelper.createHash(testSpan),
        mockSpanDB.get(0).getSpanList().get(0).getHashCode());
  }

  @Test
  void testSingleTrace() {

    final Map<String, Trace> mockSpanDB = new HashMap<>();
    Mockito.doAnswer(i -> {
      final net.explorviz.trace.persistence.dao.Trace inserted = i.getArgument(0, Trace.class);
      final String key = inserted.getLandscapeToken() + "::" + inserted.getTraceId();
      mockSpanDB.put(key, inserted);
      return Uni.createFrom().nullItem();
    }).when(this.reactiveTraceService).insert(ArgumentMatchers.any(Trace.class));

    final int spansPerTrace = 20;

    final net.explorviz.avro.Trace testTrace = TraceHelper.randomTrace(spansPerTrace);
    long t = testTrace.getStartTimeEpochMilli();
    for (final Span s : testTrace.getSpanList()) {
      t += 1;
      s.setStartTimeEpochMilli(t);
      this.inputTopic.pipeInput(s.getTraceId(), s);
    }
    this.forceSuppression(t);

    final String k = testTrace.getLandscapeToken() + "::" + testTrace.getTraceId();
    Assertions.assertEquals(1, mockSpanDB.size());
    Assertions.assertEquals(testTrace.getSpanList().size(), mockSpanDB.get(k).getSpanList().size());
  }

  @Test
  void testMultipleTraces() {
    final Map<String, Trace> mockSpanDB = new HashMap<>();

    Mockito.doAnswer(i -> {
          final Trace inserted = i.getArgument(0, Trace.class);
          final String key = inserted.getLandscapeToken() + "::" + inserted.getTraceId();
          mockSpanDB.put(key, inserted);
          return Uni.createFrom().nullItem();
        }).when(this.reactiveTraceService)
        .insert(ArgumentMatchers.any(net.explorviz.trace.persistence.dao.Trace.class));

    final int spansPerTrace = 20;
    final int traceAmount = 20;

    // Create multiple traces that happen in parallel
    final List<KeyValue<String, Span>> traces = new ArrayList<>();
    final long baseTime = TraceHelper.randomTrace(1).getStartTimeEpochMilli();
    for (int i = 0; i < traceAmount; i++) {
      final net.explorviz.avro.Trace testTrace = TraceHelper.randomTrace(spansPerTrace);
      long t = baseTime;
      for (final Span s : testTrace.getSpanList()) {
        // Keep spans in one window
        t += 2;
        s.setStartTimeEpochMilli(t);
        traces.add(new KeyValue<>(s.getTraceId(), s));
      }
    }

    this.inputTopic.pipeKeyValueList(traces);
    this.forceSuppression(baseTime);

    for (final Map.Entry<String, Trace> entry : mockSpanDB.entrySet()) {
      Assertions.assertEquals(spansPerTrace, entry.getValue().getSpanList().size());
    }

  }


  @Test
  void testOutOfWindow() {

    // callback to get traces after analysis
    final Map<String, Trace> mockSpanDB = new HashMap<>();

    Mockito.doAnswer(i -> {
          final Trace inserted = i.getArgument(0, Trace.class);
          final String key = inserted.getLandscapeToken() + "::" + inserted.getTraceId();
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("test span size " + inserted.getSpanList().size());
          }
          // mockSpanDB.computeIfPresent(key, (k, v) -> {
          // v.getSpanList().addAll(inserted.getSpanList());
          // return v;
          // });
          mockSpanDB.putIfAbsent(key, inserted);
          return Uni.createFrom().nullItem();
        }).when(this.reactiveTraceService)
        .insert(ArgumentMatchers.any(net.explorviz.trace.persistence.dao.Trace.class));

    // push spans on topic
    final int spansPerTrace = 20;

    final net.explorviz.avro.Trace testTrace = TraceHelper.randomTrace(spansPerTrace);
    long ts = TraceHelper.randomTrace(1).getStartTimeEpochMilli();
    for (int i = 0; i < testTrace.getSpanList().size(); i++) {
      final Span s = testTrace.getSpanList().get(i);
      if (i < testTrace.getSpanList().size() - 1) {
        ts += 1;
      } else {
        // Last span arrives out of window
        ts += this.windowSizeInMs;
      }
      s.setStartTimeEpochMilli(ts);
      this.inputTopic.pipeInput(s.getTraceId(), s);
    }

    this.forceSuppression(ts);

    final String k = testTrace.getLandscapeToken() + "::" + testTrace.getTraceId();
    Assertions.assertEquals(1, mockSpanDB.size());
    Assertions.assertEquals(testTrace.getSpanList().size(),
        mockSpanDB.get(k).getSpanList().size() + 1);
  }


  /**
   * Forces the suppression to emit results by sending a dummy event with a timestamp larger than
   * the suppression time.
   */
  private void forceSuppression(final long lastTimestamp) {
    final Duration secs = Duration.ofMillis(this.windowSizeInMs).plusMillis(this.graceSizeInMs);
    final Span dummy = TraceHelper.randomSpan();

    final Instant ts = TimestampHelper.toInstant(lastTimestamp).plus(secs);
    dummy.setStartTimeEpochMilli(TimestampHelper.toTimestamp(ts));
    this.inputTopic.pipeInput(dummy.getTraceId(), dummy);
  }


}
