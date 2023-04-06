package net.explorviz.trace.service.reduction;

import java.util.HashSet;
import java.util.Set;
import net.explorviz.avro.Span;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Representation for a node in a {@link CallTree}.
 */
public class CallTreeNode {


  private final Span spanDynamic;
  private final Set<CallTreeNode> callees;

  private CallTreeNode parent;


  public CallTreeNode(final Span spanDynamic) {
    this.spanDynamic = spanDynamic;
    this.callees = new HashSet<>();
  }

  /**
   * Returns the level of the current node in the tree structure. If the current node is the root
   * node, the level is 0.
   *
   * @return the level of the current node in the tree structure.
   */
  public int getLevel() {
    if (this.parent == null) {
      return 0;
    }
    return this.parent.getLevel() + 1;
  }

  public boolean isRoot() {
    return this.parent == null;
  }

  public boolean isLeaf() {
    return this.callees.isEmpty();
  }

  /* default */ CallTreeNode getParent() {
    return this.parent;
  }

  public void setParent(final CallTreeNode parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append("spanId",
            this.spanDynamic.getSpanId()).append("hashCode", this.hashCode())
        .append("callees", this.callees).toString();
  }

  public void addChild(final CallTreeNode child) {
    this.callees.add(child);
    child.setParent(this);
  }

  public String getLandscapeToken() {
    return this.spanDynamic.getLandscapeToken();
  }

  public Span getSpanDynamic() {
    return this.spanDynamic;
  }

  public Set<CallTreeNode> getCallees() {
    return this.callees;
  }


}
