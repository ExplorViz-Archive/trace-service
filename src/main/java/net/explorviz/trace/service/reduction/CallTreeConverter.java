package net.explorviz.trace.service.reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.explorviz.avro.Span;
import net.explorviz.avro.Trace;
import net.explorviz.trace.service.TraceAggregator;

/**
 * Provides static methods to convert {@link Trace}s to {@link CallTree}s and vice versa.
 */
public final class CallTreeConverter {


  private CallTreeConverter() {
    // utility class
  }

  /**
   * Converts a trace to a call tree.
   *
   * @param trace the trace to convert
   * @return the corresponding call tree
   */
  public static CallTree toTree(final Trace trace) { // NOPMD
    final HashMap<String, CallTreeNode> knownNodes = new HashMap<>();
    final List<CallTreeNode> orphans = new ArrayList<>();
    CallTreeNode root = null;

    for (final Span sd : trace.getSpanList()) {
      final String spanId = sd.getSpanId();
      final CallTreeNode node = new CallTreeNode(sd);
      knownNodes.put(spanId, node);

      final Iterator<CallTreeNode> it = orphans.iterator();
      while (it.hasNext()) {
        final CallTreeNode orphan = it.next();
        if (orphan.getSpanDynamic().getParentSpanId().equals(spanId)) {
          node.addChild(orphan);
          it.remove();
        }
      }


      if (sd.getParentSpanId().isEmpty()) {
        root = node;
        continue;
      }
      final CallTreeNode parent = knownNodes.get(sd.getParentSpanId());
      if (parent == null) {
        orphans.add(node);
      } else {
        parent.addChild(node);
      }
    }

    if (root == null) {
      throw new IllegalArgumentException("Invalid trace: No root");
    }
    if (!orphans.isEmpty()) {
      throw new IllegalArgumentException("Invalid trace: Not connected");
    }

    return new CallTree(root);
  }


  /**
   * Converts a call tree to a trace.
   *
   * @param tree the call tree
   * @return the corresponding trace
   */
  public static Trace toTrace(final CallTree tree) {
    final TraceAggregator ta = new TraceAggregator();
    final Trace t = new Trace();
    tree.bfs(n -> ta.aggregate(t, n.getSpanDynamic()));
    return t;
  }

}
