package net.explorviz.trace.persistence.dao;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import java.util.List;
import java.util.Objects;

/**
 * Bean for a Trace.
 */
@Entity
public class Trace {

  @PartitionKey
  private String landscapeToken;

  @ClusteringColumn(1)
  private long startTime;

  @ClusteringColumn(2)
  private String traceId;

  private long endTime;
  private long duration;
  private int overallRequestCount;
  private int traceCount;

  // @Frozen not available in dependendy?
  private List<SpanDynamic> spanList;

  public Trace() {
    // for serialization
  }

  /**
   * Creates a Trace object that represents a trace of spans in a distributed system.
   *
   * @param landscapeToken      the token of the landscape where the trace occurred
   * @param traceId             the ID of the trace
   * @param startTime           the start time of the trace (in milliseconds since the Unix epoch)
   * @param endTime             the end time of the trace (in milliseconds since the Unix epoch)
   * @param duration            the duration of the trace (in milliseconds)
   * @param overallRequestCount the overall number of requests that occurred during the trace
   * @param traceCount          Number of traces that this Trace object represents
   * @param spanList            the list of spans in the trace
   */
  public Trace(final String landscapeToken, final String traceId, final long startTime,
      final long endTime, final long duration, final int overallRequestCount, final int traceCount,
      final List<SpanDynamic> spanList) {
    super();
    this.landscapeToken = landscapeToken;
    this.traceId = traceId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.overallRequestCount = overallRequestCount;
    this.traceCount = traceCount;
    this.spanList = spanList;
  }

  public String getLandscapeToken() {
    return this.landscapeToken;
  }

  public void setLandscapeToken(final String landscapeToken) {
    this.landscapeToken = landscapeToken;
  }

  public String getTraceId() {
    return this.traceId;
  }

  public void setTraceId(final String traceId) {
    this.traceId = traceId;
  }

  public long getStartTime() {
    return this.startTime;
  }

  public void setStartTime(final long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return this.endTime;
  }

  public void setEndTime(final long endTime) {
    this.endTime = endTime;
  }

  public long getDuration() {
    return this.duration;
  }

  public void setDuration(final long duration) {
    this.duration = duration;
  }

  public int getOverallRequestCount() {
    return this.overallRequestCount;
  }

  public void setOverallRequestCount(final int overallRequestCount) {
    this.overallRequestCount = overallRequestCount;
  }

  public int getTraceCount() {
    return this.traceCount;
  }

  public void setTraceCount(final int traceCount) {
    this.traceCount = traceCount;
  }

  public List<SpanDynamic> getSpanList() {
    return this.spanList;
  }

  public void setSpanList(final List<SpanDynamic> spanList) {
    this.spanList = spanList;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.duration, this.endTime, this.landscapeToken, this.overallRequestCount,
        this.spanList, this.startTime, this.traceCount, this.traceId);
  }

  @Override
  public boolean equals(final Object obj) {
    // An object is always equal to itself
    if (this == obj) {
      return true;
    }
    // Ensure that objects are indeed trace objects
    if (!(obj instanceof Trace)) {
      return false;
    }

    // Compare dynamic spans with respect to their attributes
    final Trace other = (Trace) obj;
    return this.duration == other.duration
        && this.endTime == other.endTime
        && Objects.equals(this.landscapeToken, other.landscapeToken)
        && this.overallRequestCount == other.overallRequestCount
        && Objects.equals(this.spanList, other.spanList)
        && this.startTime == other.startTime
        && this.traceCount == other.traceCount
        && Objects.equals(this.traceId, other.traceId);
  }


}
