package net.explorviz.trace.service.reduction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Represents a call tree based on executed operations.
 */
public class CallTree {

  private final CallTreeNode root;

  public CallTree(final CallTreeNode root) {
    this.root = root;
  }

  /**
   * Breadth-first search algorithm that traverses the call tree. Call with a {@code Consumer}
   * function to execute additional logic. Examples that use this algorithm are the calculations for
   * size or depth.
   */
  public void bfs(final Consumer<CallTreeNode> visitor) {
    final Queue<CallTreeNode> queue = new ArrayDeque<>();
    final Set<CallTreeNode> seen = new HashSet<>();
    queue.add(this.root);
    while (!queue.isEmpty()) {
      final CallTreeNode current = queue.poll();
      seen.add(current);
      visitor.accept(current);
      for (final CallTreeNode ctn : current.getCallees()) {
        if (!seen.contains(ctn)) {
          queue.add(ctn);
        }
      }
    }
  }

  /**
   * Calculate depth (levels) of this call tree.
   *
   * @return Depth (number of levels) of this call tree as int.
   */
  public int depth() {
    final AtomicReference<Integer> maxLevel = new AtomicReference<>(0);
    this.bfs(n -> {
      final int level = n.getLevel();
      if (level > maxLevel.get()) {
        maxLevel.set(level);
      }
    });
    return maxLevel.get();
  }

  /**
   * Calculate size of this call tree.
   *
   * @return Size of this call tree as int.
   */
  public int size() {

    class Counter {

      private int val = 0;

      public int getVal() {
        return this.val;
      }

      public void inc() {
        this.val += 1;
      }
    }

    final Counter c = new Counter();
    this.bfs(callTreeNode -> c.inc());
    return c.getVal();
  }

  public CallTreeNode getRoot() {
    return this.root;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("root", this.root)
        .toString();
  }


}
