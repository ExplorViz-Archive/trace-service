package net.explorviz.trace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;
import net.explorviz.trace.helper.TraceHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceAggregatorTest {

  private static final String TEST_TRACE_ID = "tid";
  private static final String TEST_TOKEN = "tok";

  private TraceAggregator aggregator;

  @BeforeEach
  void setUp() {
    this.aggregator = new TraceAggregator();
  }

  @Test
  void newTrace() {
    final Span fresh = TraceHelper.randomSpan(TEST_TRACE_ID, TEST_TOKEN);
    final Trace traceWithSpan = this.aggregator.aggregate(new Trace(), fresh);

    assertEquals(1, traceWithSpan.getSpanList().size(), "Invalid amount of spans in trace");
    assertTrue(traceWithSpan.getSpanList().contains(fresh), "Trace does not contain first span");
    assertEquals(fresh.getStartTimeEpochMilli(), traceWithSpan.getStartTimeEpochMilli(),
        "Start time does not match");
    assertEquals(fresh.getEndTimeEpochMilli(), traceWithSpan.getEndTimeEpochMilli(), "End time does not match");
  }

  @Test
  void addEarlierSpan() {
    final Instant now = Instant.now();
    final Span first = TraceHelper.randomSpan(TEST_TRACE_ID, TEST_TOKEN);

    first.setStartTimeEpochMilli(this.toTimestamp(now.minus(1, ChronoUnit.SECONDS)));
    first.setEndTimeEpochMilli(this.toTimestamp(now));

    final Trace aggregate = this.aggregator.aggregate(new Trace(), first);

    final Span newFirst = Span.newBuilder(first)
        .setStartTimeEpochMilli(this.toTimestamp(now.minus(1, ChronoUnit.SECONDS))).build();

    this.aggregator.aggregate(aggregate, newFirst);
    assertEquals(2, aggregate.getSpanList().size(), "Invalid amount of spans in trace");
    assertEquals(aggregate.getSpanList().get(0), newFirst, "Trace does not contain first span");
  }

  @Test
  void addLaterSpan() {
    final Instant now = Instant.now();
    final Span first = TraceHelper.randomSpan(TEST_TRACE_ID, TEST_TOKEN);

    first.setStartTimeEpochMilli(this.toTimestamp(now.minus(1, ChronoUnit.SECONDS)));
    first.setEndTimeEpochMilli(this.toTimestamp(now));

    final Trace aggregate = this.aggregator.aggregate(new Trace(), first);

    final Span newLast = Span.newBuilder(first)
        .setStartTimeEpochMilli(this.toTimestamp(now.plus(1, ChronoUnit.SECONDS)))
        .setEndTimeEpochMilli(this.toTimestamp(now.plus(5, ChronoUnit.SECONDS))).build();
    this.aggregator.aggregate(aggregate, newLast);
    assertEquals(2, aggregate.getSpanList().size(), "Invalid amount of spans in trace");
    assertEquals(aggregate.getSpanList().get(1), newLast, "Trace does not contain first span");
  }



  private long toTimestamp(final Instant instant) {
    return instant.toEpochMilli();
  }


}
