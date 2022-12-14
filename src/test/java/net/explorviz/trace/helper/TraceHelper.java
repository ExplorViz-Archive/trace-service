package net.explorviz.trace.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
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

  public static Span randomSpan(final String traceId, final String token,
      final String parentSpanId) {
    final long maxSeconds = 1609459200;
    final long minSeconds = 1577836800;

    return Span.newBuilder()
        .setLandscapeToken(token)
        .setSpanId(RandomStringUtils.random(8, true, true))
        .setParentSpanId(parentSpanId)
        .setTraceId(traceId)
        .setStartTimeEpochMilli(RandomUtils.nextLong(minSeconds, maxSeconds))
        .setEndTimeEpochMilli(RandomUtils.nextLong(minSeconds, maxSeconds))
        .setFullyQualifiedOperationName(randomFqn())
        .setHostname(RandomStringUtils.randomAlphabetic(10))
        .setHostIpAddress(randomIp())
        .setAppName(RandomStringUtils.randomAlphabetic(10))
        .setAppInstanceId(RandomStringUtils.randomNumeric(3))
        .setAppLanguage(RandomStringUtils.randomAlphabetic(5))
        .build();
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
   * @param token   the token to use
   * @return a randomly generated span
   */
  public static Span randomSpan(final String traceId, final String token) {
    final long maxSeconds = 1609459200;
    final long minSeconds = 1577836800;

    return Span.newBuilder()
        .setLandscapeToken(token)
        .setSpanId(RandomStringUtils.random(8, true, true))
        .setParentSpanId(RandomStringUtils.random(8, true, true))
        .setTraceId(traceId)
        .setStartTimeEpochMilli(RandomUtils.nextLong(minSeconds, maxSeconds))
        .setEndTimeEpochMilli(RandomUtils.nextLong(minSeconds, maxSeconds))
        .setFullyQualifiedOperationName(randomFqn())
        .setHostname(RandomStringUtils.randomAlphabetic(10))
        .setHostIpAddress(randomIp())
        .setAppName(RandomStringUtils.randomAlphabetic(10))
        .setAppInstanceId(RandomStringUtils.randomNumeric(3))
        .setAppLanguage(RandomStringUtils.randomAlphabetic(5))
        .build();
  }

  /**
   * Creates a random trace such that:
   * <ul>
   * <li>The start- and end-time correspond to the span lis</li>
   * <li>All spans have the same traceId</li>
   * <li>The trace and all spans have the same landscape token</li>
   * </ul>
   * The included spans themselves are not valid, in particular
   * <ul>
   * <li>Parent span IDs are completely random do not match spans in the same trace</li>
   * <li>The hash codes are truly random</li>
   * <li>Start- and End-Times are random points anywhere in the year 2020</li>
   * </ul>
   *
   * @param spanAmount the amount of spans to include into the trace, must be at least 1
   * @return a trace with randomly filled values
   */
  public static Trace randomTrace(final int spanAmount) {

    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    long start = 0L;
    long end = 0L;
    Span root = null;
    final List<Span> spans = new ArrayList<>();

    for (int i = 0; i < spanAmount; i++) {
      String psid;
      if (root == null) {
        psid = "";
      } else {
        psid = spans.stream().map(Span::getSpanId)
            .skip(RandomUtils.nextInt(0, spans.size() - 1)).findAny().get();
      }
      final Span s = randomSpan(traceId, landscapeToken, psid);
      if (root == null) {
        root = s;
      }
      if (start == 0L || TimestampHelper.isBefore(s.getStartTimeEpochMilli(), start)) {
        start = s.getStartTimeEpochMilli();
      }
      if (end == 0L || TimestampHelper.isAfter(s.getEndTimeEpochMilli(), end)) {
        end = s.getEndTimeEpochMilli();
      }
      spans.add(s);
    }

    return Trace.newBuilder().setLandscapeToken(landscapeToken).setTraceId(traceId)
        .setStartTimeEpochMilli(start).setEndTimeEpochMilli(end)
        .setDuration(TimestampHelper.durationMs(start, end)).setSpanList(spans).setTraceCount(1)
        .setOverallRequestCount(1).build();
  }

  /**
   * Creates a trace with a `iterations` branches following a single root node. Each branch has
   * exactly `length` spans all spans on the same level share the same hashcode.
   *
   * @param iterations the number of iterations of the loop
   * @param length     the length of each iteration in spans
   * @return a trace representing the loop
   */
  public static Trace uniformLoop(final int iterations, final int length) {
    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    final List<Span> spans = new ArrayList<>(iterations * length + 1);

    final Span root = randomSpan(traceId, landscapeToken, "");
    spans.add(root);
    long start = root.getStartTimeEpochMilli();
    long end = root.getEndTimeEpochMilli();

    for (int it = 0; it < iterations; it++) {
      final Span iterationRoot = randomSpan(traceId, landscapeToken, root.getSpanId());
      String parentId = iterationRoot.getSpanId();
      spans.add(iterationRoot);
      for (int l = 1; l < length; l++) {
        final Span next = randomSpan(traceId, landscapeToken, parentId);
        if (TimestampHelper.isBefore(next.getStartTimeEpochMilli(), start)) {
          start = next.getStartTimeEpochMilli();
        }
        if (TimestampHelper.isAfter(next.getEndTimeEpochMilli(), end)) {
          end = next.getEndTimeEpochMilli();
        }
        parentId = next.getSpanId();
        spans.add(next);
      }
    }
    return Trace.newBuilder().setLandscapeToken(landscapeToken).setTraceId(traceId)
        .setStartTimeEpochMilli(start).setEndTimeEpochMilli(end)
        .setDuration(TimestampHelper.durationMs(start, end)).setSpanList(spans).setTraceCount(1)
        .setOverallRequestCount(1).build();
  }

  public static Trace linearTrace(final int spanNum) {
    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    final List<Span> spans = new ArrayList<>(spanNum);

    final Span root = randomSpan(traceId, landscapeToken, "");
    spans.add(root);
    String parentId = root.getSpanId();
    long start = root.getStartTimeEpochMilli();
    long end = root.getEndTimeEpochMilli();

    for (int i = 0; i < spanNum - 1; i++) {
      final Span next = randomSpan(traceId, landscapeToken, parentId);
      if (TimestampHelper.isBefore(next.getStartTimeEpochMilli(), start)) {
        start = next.getStartTimeEpochMilli();
      }
      if (TimestampHelper.isAfter(next.getEndTimeEpochMilli(), end)) {
        end = next.getEndTimeEpochMilli();
      }
      parentId = next.getSpanId();
      spans.add(next);
    }
    return Trace.newBuilder().setLandscapeToken(landscapeToken).setTraceId(traceId)
        .setStartTimeEpochMilli(start).setEndTimeEpochMilli(end)
        .setDuration(TimestampHelper.durationMs(start, end)).setSpanList(spans).setTraceCount(1)
        .setOverallRequestCount(1).build();
  }

  /**
   * Create a trace that represents a linear recursion, i.e., creates a linear trace with repeated
   * groups of `size` spans that share tha same hashcode. The total lenght (number of spans) is
   * `recursion`*`size`.
   *
   * @param recursions the amount of recursion
   * @param size       the size of each recursive call
   * @return the trace representing the recursion
   */
  public static Trace linearRecursion(final int recursions, final int size) {
    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    final List<Span> spans = new ArrayList<>(recursions * size + 1);

    final Span root = randomSpan(traceId, landscapeToken, "");
    spans.add(root);
    String parentId = root.getSpanId();
    long start = root.getStartTimeEpochMilli();
    long end = root.getEndTimeEpochMilli();

    for (int i = 1; i <= recursions * size; i++) {
      final Span next = randomSpan(traceId, landscapeToken, parentId);
      if (TimestampHelper.isBefore(next.getStartTimeEpochMilli(), start)) {
        start = next.getStartTimeEpochMilli();
      }
      if (TimestampHelper.isAfter(next.getEndTimeEpochMilli(), end)) {
        end = next.getEndTimeEpochMilli();
      }
      parentId = next.getSpanId();
      spans.add(next);

    }
    return Trace.newBuilder().setLandscapeToken(landscapeToken).setTraceId(traceId)
        .setStartTimeEpochMilli(start).setEndTimeEpochMilli(end)
        .setDuration(TimestampHelper.durationMs(start, end)).setSpanList(spans).setTraceCount(1)
        .setOverallRequestCount(1).build();
  }

  private static String randomFqn() {
    final String[] parts = new String[3];
    IntStream.rangeClosed(0, 2).forEach(i -> parts[i] = RandomStringUtils.randomAlphabetic(1, 10));
    return String.join(".", parts);
  }

  private static String randomIp() {
    final String[] parts = new String[4];
    IntStream.rangeClosed(0, 3).forEach(i -> parts[i] = RandomStringUtils.randomNumeric(1, 4));
    return String.join(".", parts);
  }


}
