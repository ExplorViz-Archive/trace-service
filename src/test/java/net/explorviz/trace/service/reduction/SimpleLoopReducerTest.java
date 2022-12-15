package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.explorviz.avro.Trace;
import net.explorviz.trace.helper.TraceHelper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimpleLoopReducerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLoopReducerTest.class);

  @Test
  void reduceLoops() {

    int[] loopLens = new int[] {1, 2, 4, 16, 32, 512 };
    int[] iterations = new int[] {1, 2, 10, 20, 500};

    //int[] loopLens = new int[] {1};
    //int[] iterations = new int[] {2};

    for (int it : iterations) {
      for (int len : loopLens) {
        Trace trace = TraceHelper.uniformLoop(it, len);
        CallTree tree = CallTreeConverter.toTree(trace);

        // Assert correct trace generated
        SimpleLoopReducer reducer = new SimpleLoopReducer();
        CallTree reduced = reducer.reduce(tree);
        assertEquals(len+1, reduced.size());
      }
    }

  }
}
