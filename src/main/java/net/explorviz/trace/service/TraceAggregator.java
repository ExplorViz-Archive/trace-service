package net.explorviz.trace.service;

import static net.explorviz.trace.service.TimestampHelper.durationMs;
import static net.explorviz.trace.service.TimestampHelper.isAfter;
import static net.explorviz.trace.service.TimestampHelper.isBefore;

import java.util.LinkedList;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;

/**
 * Contains methods that help to aggregate multiple span into a trace as they come in.
 */
public class TraceAggregator {


  private Trace initTrace(final Trace freshTrace, final Span firstSpan) {

    freshTrace.setTraceId(firstSpan.getTraceId());

    // Use linked list here to avoid costly reallocation of array lists
    // We don't need random access provided by array lists
    freshTrace.setSpanList(new LinkedList<>());
    freshTrace.getSpanList().add(firstSpan);

    // Set start and end time to equal to the times of the only span
    freshTrace.setStartTimeEpochMilli(firstSpan.getStartTimeEpochMilli());
    freshTrace.setEndTimeEpochMilli(firstSpan.getEndTimeEpochMilli());

    freshTrace.setDuration(
        durationMs(firstSpan.getStartTimeEpochMilli(), firstSpan.getEndTimeEpochMilli()));

    freshTrace.setOverallRequestCount(1);
    freshTrace.setTraceCount(1);

    // set initial trace id - do not change, since this is the major key for kafka
    // partitioning
    freshTrace.setTraceId(freshTrace.getTraceId());
    freshTrace.setLandscapeToken(firstSpan.getLandscapeToken());

    return freshTrace;
  }

  /**
   * Adds a {@link Span} to a given trace. Adjusts start and end times as well as requests counts of
   * the trace and takes care of new and empty traces. Additionally makes sure that spans are
   * ordered by their respective start times.
   *
   * @param aggregate the trace to add the span to
   * @param newSpan   the span to add to the trace
   * @return the trace with the span included
   */
  public Trace aggregate(final Trace aggregate, final Span newSpan) {

    if (aggregate.getSpanList() == null || aggregate.getSpanList().isEmpty()) {
      return this.initTrace(aggregate, newSpan);
    }

    // Add the span to the trace
    aggregate.getSpanList().add(newSpan);
    // Depending on the position the span was inserted, the start or end time must be adjusted
    if (isBefore(newSpan.getStartTimeEpochMilli(), aggregate.getStartTimeEpochMilli())) {
      // Span is the current earliest in the trace
      aggregate.setStartTimeEpochMilli(newSpan.getStartTimeEpochMilli());
      aggregate.setDuration(
          durationMs(aggregate.getStartTimeEpochMilli(), aggregate.getEndTimeEpochMilli()));
    }

    if (isAfter(newSpan.getEndTimeEpochMilli(), aggregate.getEndTimeEpochMilli())) {
      // Span is the current latest in the trace
      aggregate.setEndTimeEpochMilli(newSpan.getEndTimeEpochMilli());
      aggregate.setDuration(
          durationMs(aggregate.getStartTimeEpochMilli(), aggregate.getEndTimeEpochMilli()));
    }

    return aggregate;
  }


}
