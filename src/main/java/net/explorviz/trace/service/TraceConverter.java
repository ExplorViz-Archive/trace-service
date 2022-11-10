package net.explorviz.trace.service;

import java.util.ArrayList;
import java.util.List;
import net.explorviz.avro.SpanDynamic;
import net.explorviz.trace.persistence.dao.Trace;

/**
 * Utility class which converts {@link net.explorviz.avro.Trace} into {@link Trace} objects.
 */
public final class TraceConverter {

  private TraceConverter() {
    // Utility class
  }

  public static Trace convertTraceToDao(final net.explorviz.avro.Trace t) {
    // Build Dao SpanList

    final List<net.explorviz.trace.persistence.dao.SpanDynamic> daoSpanList = new ArrayList<>();

    for (final SpanDynamic span : t.getSpanList()) {

      final long startTime = span.getStartTimeEpochMilli();
      final long endTime = span.getEndTimeEpochMilli();

      final net.explorviz.trace.persistence.dao.SpanDynamic spanDynamicEntity =
          new net.explorviz.trace.persistence.dao.SpanDynamic(span.getLandscapeToken(),
              span.getSpanId(), span.getParentSpanId(), span.getTraceId(), startTime,
              endTime, span.getHashCode());

      daoSpanList.add(spanDynamicEntity);
    }

    // Build Dao Trace

    final long startTime = t.getStartTimeEpochMilli();
    final long endTime = t.getEndTimeEpochMilli();

    return new Trace(t.getLandscapeToken(), t.getTraceId(),
        startTime, endTime, t.getDuration(), t.getOverallRequestCount(),
        t.getTraceCount(), daoSpanList);
  }

}
