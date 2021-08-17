package net.explorviz.trace.service.reduction;

public interface SpanReducer {

  CallTree reduce(CallTree tree);

}
