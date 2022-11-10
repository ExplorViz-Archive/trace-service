package net.explorviz.trace.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.explorviz.avro.SpanDynamic;
import net.explorviz.avro.Trace;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

/**
 * Utility class containing helper methods to create traces and spans.
 */
public final class TraceHelper {



  /**
   * Create a random span with given trace id and landscape token such that:
   * <ul>
   * <li>Timestamps are random points in time in the year of 2020 (to avoid overflows)</li>
   * <li>Hash codes are not calculated but random strings</li>
   * </ul>
   *
   * @param traceId the trace id to use
   * @param token   the token to use
   * @param parentSpanId the id of the parent span
   * @return a randomly generated span
   */
  public static SpanDynamic randomSpan(final String traceId, final String token,
                                       final String parentSpanId) {
    final long maxSeconds = 1609459200;
    final long minSeconds = 1577836800;

    final int maxNanos = Instant.MAX.getNano();

    return SpanDynamic.newBuilder()
        .setLandscapeToken(token)
        .setStartTimeEpochMilli(RandomUtils.nextLong(minSeconds, maxSeconds))
        .setEndTimeEpochMilli(RandomUtils.nextLong(minSeconds, maxSeconds))
        .setTraceId(traceId)
        .setParentSpanId(parentSpanId)
        .setSpanId(RandomStringUtils.random(8, true, true))
        .setHashCode(RandomStringUtils.random(256, true, true))
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

  /**
   * Creates a trace with a `iterations` branches following a single root node.
   * Each branch has exactly `length` spans all spans on the same level share the same hashcode.
   * @param iterations the number of iterations of the loop
   * @param length the length of each iteration in spans
   * @return a trace representing the loop
   */
  public static Trace uniformLoop(final int iterations, final int length) {
    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    List<SpanDynamic> spans = new ArrayList<>(iterations*length+1);

    SpanDynamic root = randomSpan(traceId, landscapeToken, "");
    spans.add(root);
    long start = root.getStartTimeEpochMilli();
    long end = root.getEndTimeEpochMilli();

    // Generate 'length' hashcodes
    ArrayList<String> hashcodes = new ArrayList<>(length);
    for (int l=0; l < length; l++) {
      hashcodes.add(RandomStringUtils.random(256, true, true));
    }

    for (int it=0; it<iterations; it++) {
      SpanDynamic iterationRoot = randomSpan(traceId, landscapeToken, root.getSpanId());
      String parentId = iterationRoot.getSpanId();
      iterationRoot.setHashCode(hashcodes.get(0));
      spans.add(iterationRoot);
      for (int l=1; l<length; l++) {
        SpanDynamic next = randomSpan(traceId, landscapeToken, parentId);
        next.setHashCode(hashcodes.get(l));
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

  public static Trace linearTrace(final int spanNum) {
    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    List<SpanDynamic> spans = new ArrayList<>(spanNum);

    SpanDynamic root = randomSpan(traceId, landscapeToken, "");
    spans.add(root);
    String parentId = root.getSpanId();
    long start = root.getStartTimeEpochMilli();
    long end = root.getEndTimeEpochMilli();

    for (int i=0; i<spanNum-1; i++) {
      SpanDynamic next = randomSpan(traceId, landscapeToken, parentId);
      if (TimestampHelper.isBefore(next.getStartTimeEpochMilli(), start)) {
        start = next.getStartTimeEpochMilli();
      }
      if (TimestampHelper.isAfter(next.getEndTimeEpochMilli(), end)) {
        end = next.getEndTimeEpochMilli();
      }
      parentId = next.getSpanId();
      spans.add(next);
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

  /**
   * Create a trace that represents a linear recursion, i.e.,
   * creates a linear trace with repeated groups of `size` spans that share tha same hashcode.
   * The total lenght (number of spans) is `recursion`*`size`.
   *
   * @param recursions the amount of recursion
   * @param size the size of each recursive call
   * @return the trace representing the recursion
   */
  public static Trace linearRecursion(final int recursions, final int size) {
    final String traceId = RandomStringUtils.random(6, true, true);
    final String landscapeToken = RandomStringUtils.random(32, true, true);

    List<SpanDynamic> spans = new ArrayList<>(recursions*size+1);

    ArrayList<String> hashcodes = new ArrayList<>(size);
    for (int l=0; l < size; l++) {
      hashcodes.add(RandomStringUtils.random(256, true, true));
    }

    SpanDynamic root = randomSpan(traceId, landscapeToken, "");
    root.setHashCode(hashcodes.get(0));
    spans.add(root);
    String parentId = root.getSpanId();
    long start = root.getStartTimeEpochMilli();
    long end = root.getEndTimeEpochMilli();

    for (int i=1; i<=(recursions*size); i++) {
        SpanDynamic next = randomSpan(traceId, landscapeToken, parentId);
        next.setHashCode(hashcodes.get(i%size));
        if (TimestampHelper.isBefore(next.getStartTimeEpochMilli(), start)) {
          start = next.getStartTimeEpochMilli();
        }
        if (TimestampHelper.isAfter(next.getEndTimeEpochMilli(), end)) {
          end = next.getEndTimeEpochMilli();
        }
        parentId = next.getSpanId();
        spans.add(next);

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

  public static Trace randomTrace(final int spanAmount, final String landscapeToken) {

    final String traceId = RandomStringUtils.random(6, true, true);

    long start = 0L;
    long end = 0L;
    SpanDynamic root = null;
    final List<SpanDynamic> spans = new ArrayList<>();



    for (int i = 0; i < spanAmount; i++) {
      String psid;
      if (root == null) {
        psid = "";
      } else {
        psid = spans.stream()
            .map(SpanDynamic::getSpanId)
            .skip(RandomUtils.nextInt(0, spans.size() - 1))
            .findAny()
            .get();
      }
      final SpanDynamic s = randomSpan(traceId, landscapeToken, psid);
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
