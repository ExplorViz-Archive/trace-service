package net.explorviz.trace.service.reduction;

import static org.junit.jupiter.api.Assertions.*;

import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceHelper;
import org.junit.jupiter.api.Test;

class SimpleLoopReducerTest {

  @Test
  void reduceLoops() {
    // generate trace with higher depth
    int loopLen = 10;
    int its  = 5;
    Trace trace = TraceHelper.uniformLoop(its, loopLen);
    CallTree tree = TraceToTree.fromTrace(trace);

    // Assert correct trace generated
    SimpleLoopReducer reducer = new SimpleLoopReducer();
    CallTree reduced = reducer.reduce(tree);
    System.out.println(reduced);
    assertEquals(10+1, reduced.size());
  }
}
