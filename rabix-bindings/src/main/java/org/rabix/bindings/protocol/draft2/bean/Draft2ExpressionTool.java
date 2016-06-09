package org.rabix.bindings.protocol.draft2.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Draft2ExpressionTool extends Draft2JobApp {

  @JsonProperty("expression")
  private Object script;

  public Object getScript() {
    return script;
  }

  @Override
  @JsonIgnore
  public Draft2JobAppType getType() {
    return Draft2JobAppType.EXPRESSION_TOOL;
  }

  @Override
  public String toString() {
    return "Draft2ExpressionTool [script=" + script + ", id=" + id + ", context=" + context + ", description=" + description + ", inputs=" + getInputs() + ", outputs=" + getOutputs() + ", requirements=" + requirements + "]";
  }
  
}
