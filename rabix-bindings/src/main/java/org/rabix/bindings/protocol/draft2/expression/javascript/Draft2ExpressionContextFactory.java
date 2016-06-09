package org.rabix.bindings.protocol.draft2.expression.javascript;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.rabix.bindings.protocol.draft2.expression.Draft2ExpressionTimeoutException;

public class Draft2ExpressionContextFactory extends ContextFactory {

  private int timeoutInSeconds;
  
  public Draft2ExpressionContextFactory(int timeoutInSeconds) {
    this.timeoutInSeconds = timeoutInSeconds;
  }
  
  @SuppressWarnings("deprecation")
  private static class ExpressionContext extends Context {
    long startTime;
  }

  protected Context makeContext() {
    ExpressionContext cx = new ExpressionContext();
    cx.setInstructionObserverThreshold(1000);
    return cx;
  }

  public boolean hasFeature(Context cx, int featureIndex) {
    switch (featureIndex) {
    case Context.FEATURE_NON_ECMA_GET_YEAR:
      return true;
    case Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
      return true;
    case Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER:
      return true;
    case Context.FEATURE_PARENT_PROTO_PROPERTIES:
      return false;
    }
    return super.hasFeature(cx, featureIndex);
  }

  protected void observeInstructionCount(Context cx, int instructionCount) {
    ExpressionContext mcx = (ExpressionContext) cx;
    long currentTime = System.currentTimeMillis();
    if (currentTime - mcx.startTime > timeoutInSeconds * 1000) {
      throw new Draft2ExpressionTimeoutException("Script is running more than " + timeoutInSeconds + " seconds");
    }
  }

  protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    ExpressionContext mcx = (ExpressionContext) cx;
    mcx.startTime = System.currentTimeMillis();

    return super.doTopCall(callable, cx, scope, thisObj, args);
  }

}
