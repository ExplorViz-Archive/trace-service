package net.explorviz.trace.service.reduction;

import java.util.HashSet;
import java.util.Set;
import net.explorviz.avro.SpanDynamic;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Representation for a node in a {@link CallTree}.
 */
public class CallTreeNode {


  private final SpanDynamic spanDynamic;
  private final Set<CallTreeNode> callees;

  private CallTreeNode parent = null;


  public CallTreeNode(final SpanDynamic spanDynamic) {
    this.spanDynamic = spanDynamic;
    this.callees = new HashSet<>();
  }

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
    return this.callees.size() == 0;
  }

  /* default */ CallTreeNode getParent() {
    return this.parent;
  }

  public void setParent(final CallTreeNode parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("spanId", this.spanDynamic.getSpanId())
        .append("callees", this.callees)
        .toString();
  }

  public void addChild(final CallTreeNode child) {
    this.callees.add(child);
    child.setParent(this);
  }

  public String getLandscapeToken() {
    return this.spanDynamic.getLandscapeToken();
  }

  public SpanDynamic getSpanDynamic() {
    return this.spanDynamic;
  }

  public Set<CallTreeNode> getCallees() {
    return this.callees;
  }


}
