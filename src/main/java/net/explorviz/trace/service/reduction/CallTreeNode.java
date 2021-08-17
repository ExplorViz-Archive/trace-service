package net.explorviz.trace.service.reduction;

import java.util.HashSet;
import java.util.Set;
import net.explorviz.avro.SpanDynamic;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CallTreeNode {


  private SpanDynamic spanDynamic;
  private Set<CallTreeNode> callees;

  private CallTreeNode parent = null;


  public CallTreeNode(final SpanDynamic spanDynamic) {
    this.spanDynamic = spanDynamic;
    callees = new HashSet<>();
  }

  public int getLevel() {
    if (this.parent == null) {
      return 0;
    }
    return parent.getLevel()+1;
  }

  public boolean isRoot() {
    return this.parent == null;
  }

  /*default */ CallTreeNode getParent() {
    return parent;
  }

  public void setParent(final CallTreeNode parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("spanId", this.spanDynamic.getSpanId())
        .append("callees", callees)
        .toString();
  }

  public void addChild(CallTreeNode child) {
    this.callees.add(child);
    child.setParent(this);
  }

  public SpanDynamic getSpanDynamic() {
    return spanDynamic;
  }

  public Set<CallTreeNode> getCallees() {
    return callees;
  }


}
