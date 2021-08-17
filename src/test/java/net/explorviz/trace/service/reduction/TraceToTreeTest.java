package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.*;

import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceHelper;
import org.junit.jupiter.api.Test;

class TraceToTreeTest {

  @Test
  void fromTrace() {
    int[] sizes = new int[]{1, 2, 10, 100, 1000, 10000};
    for (int size: sizes) {
      Trace t = TraceHelper.randomTrace(size);
      CallTree tree = TraceToTree.fromTrace(t);
      assertEquals(size, tree.size());
    }
  }
}
