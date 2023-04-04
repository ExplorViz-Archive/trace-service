package net.explorviz.trace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.explorviz.avro.Span;
import net.explorviz.trace.helper.TraceHelper;
import net.explorviz.trace.persistence.dao.SpanDynamic;
import net.explorviz.trace.persistence.dao.Trace;
import org.junit.jupiter.api.Test;

public class TraceConverterTest {

  @Test
  void testSimpleTraceConversion() {

    final net.explorviz.avro.Trace testObject = TraceHelper.randomTrace(1);
    final Span testObjectSpan = testObject.getSpanList().get(0);

    final Trace expected = new Trace();
    expected.setLandscapeToken(testObject.getLandscapeToken());
    expected.setTraceId(testObject.getTraceId());
    expected.setStartTime(testObject.getStartTimeEpochMilli());
    expected.setEndTime(testObject.getEndTimeEpochMilli());
    expected.setDuration(testObject.getDuration());
    expected.setOverallRequestCount(testObject.getOverallRequestCount());
    expected.setTraceCount(testObject.getTraceCount());

    final SpanDynamic expectedSpan = new SpanDynamic();
    expectedSpan.setLandscapeToken(testObjectSpan.getLandscapeToken());

    expectedSpan.setTraceId(testObjectSpan.getTraceId());
    expectedSpan.setSpanId(testObjectSpan.getSpanId());
    expectedSpan.setParentSpanId(testObjectSpan.getParentSpanId());
    expectedSpan.setStartTime(testObjectSpan.getStartTimeEpochMilli());
    expectedSpan.setEndTime(testObjectSpan.getEndTimeEpochMilli());
    expectedSpan.setHashCode(HashHelper.createHash(testObjectSpan));

    final List<SpanDynamic> expectedSpanList = new ArrayList<>();
    expectedSpanList.add(expectedSpan);

    expected.setSpanList(expectedSpanList);

    final Trace result = TraceConverter.convertTraceToDao(testObject);

    assertEquals(expected, result);
  }

  @Test
  void testComplexTraceConversion() {

    final net.explorviz.avro.Trace testObject = TraceHelper.randomTrace(20);

    final Trace expected = new Trace();
    expected.setLandscapeToken(testObject.getLandscapeToken());
    expected.setTraceId(testObject.getTraceId());
    expected.setStartTime(testObject.getStartTimeEpochMilli());
    expected.setEndTime(testObject.getEndTimeEpochMilli());
    expected.setDuration(testObject.getDuration());
    expected.setOverallRequestCount(testObject.getOverallRequestCount());
    expected.setTraceCount(testObject.getTraceCount());

    final List<SpanDynamic> expectedSpanList = new ArrayList<>();

    for (Span testObjectSpan : testObject.getSpanList()) {

      final SpanDynamic expectedSpan = new SpanDynamic();
      expectedSpan.setLandscapeToken(testObjectSpan.getLandscapeToken());
      expectedSpan.setTraceId(testObjectSpan.getTraceId());
      expectedSpan.setSpanId(testObjectSpan.getSpanId());
      expectedSpan.setParentSpanId(testObjectSpan.getParentSpanId());
      expectedSpan.setStartTime(testObjectSpan.getStartTimeEpochMilli());
      expectedSpan.setEndTime(testObjectSpan.getEndTimeEpochMilli());
      expectedSpan.setHashCode(HashHelper.createHash(testObjectSpan));

      expectedSpanList.add(expectedSpan);
    }

    expected.setSpanList(expectedSpanList);

    final Trace result = TraceConverter.convertTraceToDao(testObject);

    assertEquals(expected, result);
  }


}
