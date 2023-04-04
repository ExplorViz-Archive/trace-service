package net.explorviz.trace.service;

import java.time.Duration;
import java.time.Instant;


/**
 * Helper class for timestamps.
 */
public final class TimestampHelper {

  private TimestampHelper() {}

  /**
   * Checks if the first timestamp is before than the second or the same.
   *
   * @param epochMilliOne the first timestamp
   * @param epochMilliTwo the second timestamp
   * @return true iff first <= second
   */
  public static boolean isBeforeOrEqual(final long epochMilliOne, final long epochMilliTwo) {
    return isBefore(epochMilliOne, epochMilliTwo) || isEqual(epochMilliOne, epochMilliTwo);
  }

  /**
   * Checks if the first timestamp is before than the second.
   *
   * @param one the first timestamp
   * @param two the second timestamp
   * @return true iff first < second
   */
  public static boolean isBefore(final long one, final long two) {
    final Instant f = toInstant(one);
    final Instant s = toInstant(two);
    return f.isBefore(s);
  }

  /**
   * Checks if the first timestamp is after than the second.
   *
   * @param one the first timestamp
   * @param two the second timestamp
   * @return true iff first > second
   */
  public static boolean isAfter(final long one, final long two) {
    final Instant f = toInstant(one);
    final Instant s = toInstant(two);
    return f.isAfter(s);
  }

  /**
   * Checks if the first timestamp is after than the second or the same.
   *
   * @param one the first timestamp
   * @param two the second timestamp
   * @return true iff first >= second
   */
  public static boolean isAfterOrEqual(final long one, final long two) {
    return isAfter(one, two) || isEqual(one, two);
  }

  public static boolean isEqual(final long one, final long two) {
    final Instant f = toInstant(one);
    final Instant s = toInstant(two);
    return f.equals(s);
  }

  /**
   * Calculates the duration between two timestamps in milliseconds.
   *
   * @param t1 the first timestamp
   * @param t2 the second timestamp
   * @return the duration in milliseconds
   */
  public static long durationMs(final long t1, final long t2) {
    final Instant s = toInstant(t1);
    final Instant e = toInstant(t2);
    return Duration.between(s, e).toMillis();
  }

  /**
   * Converts a timestamp to an instant. Converse of {@link #toTimestamp(Instant)}.
   *
   * @param t the timestamp
   * @return an instant representing the exact same time as the timestamp
   */
  public static Instant toInstant(final long t) {
    return Instant.ofEpochMilli(t);
  }

  /**
   * Converts an instant to a timestamp.
   *
   * @param i the instant
   * @return a timestamp representing the exact same time as the instant
   */
  public static long toTimestamp(final Instant i) {
    return i.toEpochMilli();
  }

}
