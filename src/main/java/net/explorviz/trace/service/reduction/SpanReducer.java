package net.explorviz.trace.service.reduction;

/**
 * {@link SpanReducer} interface that can be implemented to include new span reduction techniques.
 */
public interface SpanReducer {

  CallTree reduce(CallTree tree);

}
