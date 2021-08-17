package net.explorviz.trace.service.reduction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CallTree {

  private CallTreeNode root;

  public CallTree(final CallTreeNode root) {
    this.root = root;
  }

  public int size() {
    int nodes = 0;
    Queue<CallTreeNode> queue = new ArrayDeque<>();
    Set<CallTreeNode> seen = new HashSet<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      CallTreeNode current = queue.poll();
      seen.add(current);
      nodes+=1;
      for (CallTreeNode ctn: current.getCallees()) {
        if (!seen.contains(ctn)) {
          queue.add(ctn);
        }
      }
    }
    return nodes;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("root", root)
        .toString();
  }
}
