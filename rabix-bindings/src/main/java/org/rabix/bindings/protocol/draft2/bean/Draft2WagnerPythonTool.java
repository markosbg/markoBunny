package org.rabix.bindings.protocol.draft2.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Draft2WagnerPythonTool extends Draft2JobApp {

  @JsonProperty("function")
  private Object function;
  
  public Object getFunction() {
    return function;
  }

  @Override
  @JsonIgnore
  public Draft2JobAppType getType() {
    return Draft2JobAppType.WAGNER_PYTHON_TOOL;
  }

  @Override
  public String toString() {
    return "Draft2WagnerPythonTool [function=" + function + ", id=" + id + ", getInputs()=" + getInputs()
        + ", getOutputs()=" + getOutputs() + "]";
  }

}
