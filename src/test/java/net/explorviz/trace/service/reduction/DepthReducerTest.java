package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.*;

import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceHelper;
import org.junit.jupiter.api.Test;

class DepthReducerTest {

  @Test
  void reduce() {

    int[] depthLimits = new int[]{0, 1, 2, 10, 20};
    for (int limit: depthLimits) {
      // generate trace with higher depth
      int spans = 1+ limit*2;
      Trace trace = TraceHelper.randomTrace(spans);
      CallTree tree = TraceToTree.fromTrace(trace);
      // Bad, refactor to create traces with desired depth directly
      while (tree.depth() < limit) {
        spans*=2;
        trace = TraceHelper.randomTrace(spans);
        tree = TraceToTree.fromTrace(trace);
      }

      DepthReducer reducer = new DepthReducer(limit);
      CallTree reduced = reducer.reduce(tree);
      assertEquals(limit, reduced.depth());
    }
  }

  @Test
  void invalidLimit() {
    assertThrows(IllegalArgumentException.class, () -> new DepthReducer(-1));
  }
}
