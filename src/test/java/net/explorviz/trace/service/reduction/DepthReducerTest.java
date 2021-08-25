package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.*;

import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceHelper;
import org.junit.jupiter.api.Test;

class DepthReducerTest {

  @Test
  void reduceLinear() {

    int[] depthLimits = new int[]{ 1, 2, 10, 20, 200};
    for (int limit: depthLimits) {
      // generate trace with higher depth
      int spans = 1 + (limit*2);
      Trace trace = TraceHelper.linearTrace(spans);
      CallTree tree = CallTreeConverter.toTree(trace);

      // Assert correct trace generated
      assertTrue(tree.depth() >= spans-1);
      DepthReducer reducer = new DepthReducer(limit);
      CallTree reduced = reducer.reduce(tree);
      assertEquals(limit, reduced.depth());
    }
  }

  @Test
  void reduceLoop() {

    int[] depthLimits = new int[]{0, 1, 2, 10, 20, 200};
    for (int limit: depthLimits) {
      // generate trace with higher depth
      int loopLen = 1 + (limit*2);
      int its  = 5;
      Trace trace = TraceHelper.uniformLoop(its, loopLen);
      CallTree tree = CallTreeConverter.toTree(trace);

      // Assert correct trace generated
      assertTrue(tree.depth() >= loopLen);
      DepthReducer reducer = new DepthReducer(limit);
      CallTree reduced = reducer.reduce(tree);
      assertEquals(limit, reduced.depth());
    }
  }

  @Test
  void reduceRecursion() {
    // Equivalent to linear but checking the implementation of random recursion
    int[] depthLimits = new int[]{0, 1, 2, 10, 20, 50};
    for (int limit: depthLimits) {
      // generate trace with higher depth
      int size = limit+1;
      int recs  = limit+1;
      Trace trace = TraceHelper.linearRecursion(recs, size);
      CallTree tree = CallTreeConverter.toTree(trace);

      // Assert correct trace generated
      assertTrue(tree.depth() >= size*recs);
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
