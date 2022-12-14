package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;
import net.explorviz.trace.helper.TraceHelper;
import org.junit.jupiter.api.Test;

class CallTreeConverterTest {

  @Test
  void testSizeAfterConversion() {
    int[] sizes = new int[]{1, 2, 10, 100, 1000, 10000};
    for (int size: sizes) {
      Trace t = TraceHelper.randomTrace(size);
      CallTree tree = CallTreeConverter.toTree(t);
      assertEquals(size, tree.size());
    }
  }

  @Test
  void testConversion() {
    int[] sizes = new int[]{1, 2, 10, 100, 1000, 10000};
    for (int size: sizes) {
      Trace t = TraceHelper.randomTrace(size);
      CallTree tree = CallTreeConverter.toTree(t);
      Trace got = CallTreeConverter.toTrace(tree);

      List<Span> expected = t.getSpanList();
      Collections.sort(expected);

      List<Span> gotList = got.getSpanList();
      Collections.sort(gotList);

      assertEquals(expected, gotList);

      assertEquals(t.getLandscapeToken(), got.getLandscapeToken());
      assertEquals(t.getTraceId(), got.getTraceId());
      assertEquals(t.getStartTimeEpochMilli(), got.getStartTimeEpochMilli());
      assertEquals(t.getEndTimeEpochMilli(), got.getEndTimeEpochMilli());
      assertEquals(t.getDuration(), got.getDuration());
      assertEquals(t.getOverallRequestCount(), got.getOverallRequestCount());
      assertEquals(t.getTraceCount(), got.getTraceCount());
    }
  }


}
