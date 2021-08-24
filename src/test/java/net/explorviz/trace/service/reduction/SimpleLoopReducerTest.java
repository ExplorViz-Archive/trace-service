package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceHelper;
import org.junit.jupiter.api.Test;

class SimpleLoopReducerTest {

  @Test
  void reduceLoops() {

    int[] loopLens = new int[] {1, 2, 4, 16, 32, 512, 1024};
    int[] its = new int[] {1, 2, 10, 20, 1000};

    for (int it : its) {
      for (int len : loopLens) {
        Trace trace = TraceHelper.uniformLoop(it, len);
        CallTree tree = TraceConverter.toTree(trace);

        // Assert correct trace generated
        SimpleLoopReducer reducer = new SimpleLoopReducer();
        CallTree reduced = reducer.reduce(tree);
        assertEquals(len+1, reduced.size());
      }
    }

  }
}
