package org.rabix.bindings;

import java.util.List;

import org.rabix.bindings.model.Job;

public interface ProtocolCommandLineBuilder {

  String buildCommandLine(Job job) throws BindingException;
  
  List<String> buildCommandLineParts(Job job) throws BindingException;
  
}
