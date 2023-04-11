package net.explorviz.trace.persistence.dao;

import com.datastax.oss.driver.api.mapper.annotations.Entity;
import java.util.Objects;

/**
 * Bean for dynamic Span data.
 */
@Entity
public class SpanDynamic {

  private String landscapeToken;

  private String traceId;

  private String spanId;
  private String parentSpanId;

  private long startTime;

  private long endTime;
  private String hashCode; // NOPMD

  public SpanDynamic() {
    // for serialization
  }

  /**
   * Constructs a new instance of {@code SpanDynamic} with the specified parameters.
   *
   * @param landscapeToken the landscape token of the span
   * @param spanId         the ID of the span
   * @param parentSpanId   the ID of the span's parent span
   * @param traceId        the ID of the trace to which the span belongs
   * @param startTime      the start time of the span in UNIX timestamp format (milliseconds)
   * @param endTime        the end time of the span in UNIX timestamp format (milliseconds)
   * @param hashCode       the hash code of the span
   */
  public SpanDynamic(final String landscapeToken, final String spanId, final String parentSpanId,
      final String traceId, final long startTime, final long endTime, final String hashCode) {
    super();
    this.landscapeToken = landscapeToken;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
    this.traceId = traceId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.hashCode = hashCode;
  }



  public String getLandscapeToken() {
    return this.landscapeToken;
  }

  public void setLandscapeToken(final String landscapeToken) {
    this.landscapeToken = landscapeToken;
  }

  public String getSpanId() {
    return this.spanId;
  }

  public void setSpanId(final String spanId) {
    this.spanId = spanId;
  }

  public String getParentSpanId() {
    return this.parentSpanId;
  }

  public void setParentSpanId(final String parentSpanId) {
    this.parentSpanId = parentSpanId;
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

  public String getHashCode() {
    return this.hashCode;
  }

  public void setHashCode(final String hashCode) {
    this.hashCode = hashCode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.endTime, this.hashCode, this.landscapeToken, this.parentSpanId,
        this.spanId, this.startTime, this.traceId);
  }

  @Override
  public boolean equals(final Object obj) {
    // An object is always equal to itself
    if (this == obj) {
      return true;
    }

    // Ensure that objects are indeed dynamic span objects
    if (!(obj instanceof SpanDynamic)) {
      return false;
    }

    // Compare dynamic spans with respect to their attributes
    final SpanDynamic other = (SpanDynamic) obj;
    return this.startTime == other.startTime && this.endTime == other.endTime && Objects.equals(
        this.hashCode, other.hashCode) && Objects.equals(this.landscapeToken, other.landscapeToken)
        && Objects.equals(this.parentSpanId, other.parentSpanId) && Objects.equals(this.spanId,
        other.spanId) && Objects.equals(this.traceId, other.traceId);
  }

}
