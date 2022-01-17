package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.*;

import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceHelper;
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
      assertEquals(size, got.getSpanList().size());
    }
  }


}
