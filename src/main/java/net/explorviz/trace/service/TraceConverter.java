package net.explorviz.trace.service;

import java.util.ArrayList;
import java.util.List;
import net.explorviz.avro.Span;
import net.explorviz.trace.persistence.dao.SpanDynamic;
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

    final List<SpanDynamic> daoSpanList = new ArrayList<>();

    for (final Span span : t.getSpanList()) {

      final long startTime = span.getStartTimeEpochMilli();
      final long endTime = span.getEndTimeEpochMilli();

      final SpanDynamic spanDynamicEntity =
          new SpanDynamic(span.getLandscapeToken(), span.getSpanId(), span.getParentSpanId(),
              span.getTraceId(), startTime, endTime, HashHelper.createHash(span));

      daoSpanList.add(spanDynamicEntity);
    }

    // Build Dao Trace

    final long startTime = t.getStartTimeEpochMilli();
    final long endTime = t.getEndTimeEpochMilli();

    return new Trace(t.getLandscapeToken(), t.getTraceId(), startTime, endTime, t.getDuration(),
        t.getOverallRequestCount(), t.getTraceCount(), daoSpanList);
  }

}
