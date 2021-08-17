package net.explorviz.trace.service.reduction;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DepthReducer implements SpanReducer {


  private final int depthLimit;

  public DepthReducer(final int depthLimit) {
    this.depthLimit = depthLimit;
    if (this.depthLimit < 0) {
      throw new IllegalArgumentException("Depth limit must at least be 1");
    }
  }

  @Override
  public CallTree reduce(final CallTree tree) {
    HashMap<String, CallTreeNode> reduced = new HashMap<>();
    final AtomicReference<String> rootId = new AtomicReference<>();
    tree.bfs(callTreeNode -> {
      if (callTreeNode.getLevel() <= depthLimit) {
        // Create new node and save in map
        CallTreeNode cp = new CallTreeNode(callTreeNode.getSpanDynamic());
        reduced.put(cp.getSpanDynamic().getSpanId(), cp);

        // Save root
        if (callTreeNode.isRoot()) {
          rootId.set(callTreeNode.getSpanDynamic().getSpanId());
        } else {
          // Search for parent and add current as child
          // parent must exist since we perform bfs
          CallTreeNode parent = reduced.get(cp.getSpanDynamic().getParentSpanId());
          parent.addChild(cp);
        }


      }
    });
    return new CallTree(reduced.get(rootId.get()));
  }

}
