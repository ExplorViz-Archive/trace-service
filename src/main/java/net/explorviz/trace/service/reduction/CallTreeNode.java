package net.explorviz.trace.service.reduction;

import java.util.HashSet;
import java.util.Set;
import net.explorviz.avro.SpanDynamic;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CallTreeNode {


  private SpanDynamic spanDynamic;
  private Set<CallTreeNode> callees;

  public CallTreeNode(final SpanDynamic spanDynamic) {
    this.spanDynamic = spanDynamic;
    callees = new HashSet<>();
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
  }

  public SpanDynamic getSpanDynamic() {
    return spanDynamic;
  }

  public Set<CallTreeNode> getCallees() {
    return callees;
  }
}
