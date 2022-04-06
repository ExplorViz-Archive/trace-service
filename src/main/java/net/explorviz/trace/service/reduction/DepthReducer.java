package net.explorviz.trace.service.reduction;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;


/**
 * {@link SpanReducer} implementation that expects a {@link CallTree} with at least depth of 1. This
 * reducer then proceeds to cut off levels of a call tree that are deeper that the passed depth
 * limit.
 */
@ApplicationScoped
public class DepthReducer implements SpanReducer {

  @ConfigProperty(name = "explorviz.reduction.depthlimit")
  /* default */ int depthLimit; // NOCS

  public DepthReducer() {
    // for injection
  }

  public DepthReducer(final int depthLimit) {
    this.depthLimit = depthLimit;
    if (this.depthLimit < 0) {
      throw new IllegalArgumentException("Depth limit must at least be 1");
    }
  }


  @Override
  public CallTree reduce(final CallTree tree) {
    final HashMap<String, CallTreeNode> reduced = new HashMap<>();
    final AtomicReference<String> rootId = new AtomicReference<>();
    tree.bfs(callTreeNode -> {
      if (callTreeNode.getLevel() <= this.depthLimit) {
        // Create new node and save in map
        final CallTreeNode cp = new CallTreeNode(callTreeNode.getSpanDynamic());
        reduced.put(cp.getSpanDynamic().getSpanId(), cp);

        // Save root
        if (callTreeNode.isRoot()) {
          rootId.set(callTreeNode.getSpanDynamic().getSpanId());
        } else {
          // Search for parent and add current as child
          // parent must exist since we perform bfs
          final CallTreeNode parent = reduced.get(cp.getSpanDynamic().getParentSpanId());
          parent.addChild(cp);
        }


      }
    });
    return new CallTree(reduced.get(rootId.get()));
  }

}
