package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;
import net.explorviz.trace.helper.TraceHelper;
import org.junit.jupiter.api.Test;

class CallTreeConverterTest {

  @Test
  void testSizeAfterSimpleConversion() {
    int[] sizes = new int[] {1, 2, 10, 100, 1000, 10000};
    for (int size : sizes) {
      Trace t = TraceHelper.randomTrace(size);
      CallTree tree = CallTreeConverter.toTree(t);
      assertEquals(size, tree.size());
    }
  }

  @Test
  void testSizeAfterUniformLoopConversion() {

    int[] loopLens = new int[] {1, 2, 4, 16, 32, 512};
    int[] iterations = new int[] {1, 2, 10, 20, 500};

    for (int it : iterations) {
      for (int len : loopLens) {
        Trace trace = TraceHelper.uniformLoop(it, len);

        final int expectedTreeSize = trace.getSpanList().size();

        CallTree tree = CallTreeConverter.toTree(trace);
        assertEquals(expectedTreeSize, tree.size());
      }
    }
  }

  @Test
  void testDepthAfterUniformLoopConversion() {

    int[] loopLens = new int[] {1, 2, 4, 16, 32, 512};
    int[] iterations = new int[] {1, 2, 10, 20, 500};

    for (int it : iterations) {
      for (int len : loopLens) {
        Trace trace = TraceHelper.uniformLoop(it, len);

        // find different hashcodes
        List<String> differentHashcodes = new ArrayList<>();
        for (Span s : trace.getSpanList()) {
          if (!differentHashcodes.contains(s.getHashCode())) {
            differentHashcodes.add(s.getHashCode());
          }
        }

        // depth = different hashcodes - 1 (because of root span)

        CallTree tree = CallTreeConverter.toTree(trace);
        assertEquals(differentHashcodes.size() - 1, tree.depth());
      }
    }
  }

  @Test
  void testConversion() {
    int[] sizes = new int[] {1, 2, 10, 100, 1000, 10000};
    for (int size : sizes) {
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
