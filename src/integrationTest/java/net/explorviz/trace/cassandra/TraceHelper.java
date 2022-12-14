package net.explorviz.trace.cassandra;

import java.util.ArrayList;
import java.util.List;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TimestampHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

/**
 * Utility class containing helper methods to create traces and spans.
 */
public final class TraceHelper {


  /**
   * Shortcut for {@link #randomSpan(String, String)} with random trace id and landscape token.
   */
  public static Span randomSpan() {
    final String randomTraceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);
    return randomSpan(randomTraceId, landscapeToken);
  }

  /**
   * Create a random span with given trace id and landscape token such that:
   * <ul>
   * <li>Timestamps are random points in time in the year of 2020 (to avoid overflows)</li>
   * <li>Parent span IDs are completely random Ids</li>
   * <li>Hash codes are not calculated but random strings</li>
   * </ul>
   *
   * @param traceId the trace id to use
   * @param token the token to use
   * @return a randomly generated span
   */
  public static Span randomSpan(final String traceId, final String token) {
    final long maxSecondsInEpochMilli = 1609459200000L;
    final long minSecondsInEpochMilli = 1577836800000L;

    return Span.newBuilder()
        .setLandscapeToken(token)
        .setSpanId(RandomStringUtils.random(8, true, true))
        .setParentSpanId(RandomStringUtils.random(8, true, true))
        .setTraceId(traceId)
        .setStartTimeEpochMilli(RandomUtils.nextLong(minSecondsInEpochMilli, maxSecondsInEpochMilli))
        .setEndTimeEpochMilli(RandomUtils.nextLong(minSecondsInEpochMilli, maxSecondsInEpochMilli))
        .build();

  }

  public static Span randomSpanFixedTimeInterval(final String traceId, final String token,
      final long fromEpochMilli, final long toEpochMilli) {

    return Span.newBuilder()
        .setLandscapeToken(token)
        .setStartTimeEpochMilli(fromEpochMilli)
        .setEndTimeEpochMilli(toEpochMilli)
        .setTraceId(traceId)
        .setParentSpanId(RandomStringUtils.random(8, true, true))
        .setSpanId(RandomStringUtils.random(8, true, true))
        .build();
  }

  /**
   * Creates a random trace such that:
   * <ul>
   * <li>The start- and end-time correspond to the span list</li>
   * <li>All spans have the same traceId</li>
   * <li>The trace and all spans have the same landscape token</li>
   * </ul>
   * The included spans themselves are not valid, in particular
   * <ul>
   * <li>Parent span IDs are completely random do not match spans in the same trace</li>
   * <li>The hash codes are truly random</li>
   * <li>Start- and End-Times (of spans) are random points anywhere in the year 2020</li>
   * </ul>
   *
   * @param spanAmount the amount of spans to include into the trace, must be at least 1
   * @return a trace with randomly filled values
   */
  public static Trace randomTrace(final int spanAmount) {

    final String landscapeToken = RandomStringUtils.random(32, true, true);

    return randomTrace(spanAmount, landscapeToken);

  }

  public static Trace randomTrace(final int spanAmount, final String landscapeToken) {

    final String traceId = RandomStringUtils.random(6, true, true);

    long start = 0L;
    long end = 0L;
    final List<Span> spans = new ArrayList<>();
    for (int i = 0; i < spanAmount; i++) {
      final Span s = randomSpan(traceId, landscapeToken);
      if (start == 0L || TimestampHelper.isBefore(s.getStartTimeEpochMilli(), start)) {
        start = s.getStartTimeEpochMilli();
      }
      if (end == 0L || TimestampHelper.isAfter(s.getEndTimeEpochMilli(), end)) {
        end = s.getEndTimeEpochMilli();
      }
      spans.add(s);
    }

    return Trace.newBuilder()
        .setLandscapeToken(landscapeToken)
        .setTraceId(traceId)
        .setStartTimeEpochMilli(start)
        .setEndTimeEpochMilli(end)
        .setDuration(TimestampHelper.durationMs(start, end))
        .setSpanList(spans)
        .setTraceCount(1)
        .setOverallRequestCount(1)
        .build();
  }

  public static Trace randomTrace(final int spanAmount, final String landscapeToken,
      final long fromEpochMilli, final long toEpochMilli) {

    final String traceId = RandomStringUtils.random(6, true, true);

    long start = 0L;
    long end = 0L;
    final List<Span> spans = new ArrayList<>();
    for (int i = 0; i < spanAmount; i++) {
      final Span s =
          randomSpanFixedTimeInterval(traceId, landscapeToken, fromEpochMilli, toEpochMilli);
      if (start == 0L || TimestampHelper.isBefore(s.getStartTimeEpochMilli(), start)) {
        start = s.getStartTimeEpochMilli();
      }
      if (end == 0L || TimestampHelper.isAfter(s.getEndTimeEpochMilli(), end)) {
        end = s.getEndTimeEpochMilli();
      }
      spans.add(s);
    }

    return Trace.newBuilder()
        .setLandscapeToken(landscapeToken)
        .setTraceId(traceId)
        .setStartTimeEpochMilli(start)
        .setEndTimeEpochMilli(end)
        .setDuration(TimestampHelper.durationMs(start, end))
        .setSpanList(spans)
        .setTraceCount(1)
        .setOverallRequestCount(1)
        .build();
  }


}
