package net.explorviz.trace.service.reduction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CallTree {

  private CallTreeNode root;

  public CallTree(final CallTreeNode root) {
    this.root = root;
  }

  public void bfs(Consumer<CallTreeNode> visitor) {
    Queue<CallTreeNode> queue = new ArrayDeque<>();
    Set<CallTreeNode> seen = new HashSet<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      CallTreeNode current = queue.poll();
      seen.add(current);
      visitor.accept(current);
      for (CallTreeNode ctn: current.getCallees()) {
        if (!seen.contains(ctn)) {
          queue.add(ctn);
        }
      }
    }
  }

  public int depth() {
    AtomicReference<Integer> maxLevel = new AtomicReference<>(0);
    bfs(n -> {
      int level = n.getLevel();
      if (level > maxLevel.get()){
        maxLevel.set(level);
      }
    });
    return maxLevel.get();
  }

  public int size() {
    class Counter {
      private int val = 0;

      public int getVal() {
        return val;
      }

      public void inc() {
        this.val += 1;
      }
    }
    Counter c = new Counter();
    bfs(callTreeNode -> c.inc());
    return c.getVal();
  }

  public CallTreeNode getRoot() {
    return root;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("root", root)
        .toString();
  }


}
