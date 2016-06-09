package org.rabix.bindings.model.dag;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rabix.bindings.model.Application;
import org.rabix.bindings.model.LinkMerge;
import org.rabix.bindings.model.ScatterMethod;
import org.rabix.bindings.model.dag.DAGLinkPort.LinkPortType;

public class DAGNode {

  protected final String id;
  protected final Application app;
  protected final ScatterMethod scatterMethod;
  protected final List<DAGLinkPort> inputPorts;
  protected final List<DAGLinkPort> outputPorts;
  
  protected final Map<String, Object> defaults;

  public DAGNode(String id, List<DAGLinkPort> inputPorts, List<DAGLinkPort> outputPorts, ScatterMethod scatterMethod, Application app, Map<String, Object> defaults) {
    this.id = id;
    this.app = app;
    this.inputPorts = inputPorts;
    this.outputPorts = outputPorts;
    this.scatterMethod = scatterMethod;
    this.defaults = defaults;
  }

  public String getId() {
    return id;
  }

  public Application getApp() {
    return app;
  }

  public List<DAGLinkPort> getInputPorts() {
    return inputPorts;
  }

  public List<DAGLinkPort> getOutputPorts() {
    return outputPorts;
  }
  
  public ScatterMethod getScatterMethod() {
    return scatterMethod;
  }
  
  public LinkMerge getLinkMerge(String portId, LinkPortType linkPortType) {
    switch (linkPortType) {
    case INPUT:
      for (DAGLinkPort inputPort : inputPorts) {
        if (inputPort.getId().equals(portId)) {
          return inputPort.getLinkMerge();
        }
      }
      break;
    case OUTPUT:
      for (DAGLinkPort inputPort : outputPorts) {
        if (inputPort.getId().equals(portId)) {
          return inputPort.getLinkMerge();
        }
      }
      break;
    default:
      break;
    }
    return null;
  }
  
  public Set<LinkMerge> getLinkMergeSet(LinkPortType linkPortType) {
    Set<LinkMerge> linkMergeSet = new HashSet<>();
    
    switch (linkPortType) {
    case INPUT:
      for (DAGLinkPort inputPort : inputPorts) {
        linkMergeSet.add(inputPort.getLinkMerge());
      }
      break;
    case OUTPUT:
      for (DAGLinkPort outputPort : outputPorts) {
        linkMergeSet.add(outputPort.getLinkMerge());
      }
      break;
    default:
      break;
    }
    return linkMergeSet;
  }
  
  public Map<String, Object> getDefaults() {
    return defaults;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((app == null) ? 0 : app.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((inputPorts == null) ? 0 : inputPorts.hashCode());
    result = prime * result + ((outputPorts == null) ? 0 : outputPorts.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DAGNode other = (DAGNode) obj;
    if (app == null) {
      if (other.app != null)
        return false;
    } else if (!app.equals(other.app))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (inputPorts == null) {
      if (other.inputPorts != null)
        return false;
    } else if (!inputPorts.equals(other.inputPorts))
      return false;
    if (outputPorts == null) {
      if (other.outputPorts != null)
        return false;
    } else if (!outputPorts.equals(other.outputPorts))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "DAGNode [id=" + id + ", scatterMethod=" + scatterMethod + ", inputPorts=" + inputPorts + ", outputPorts=" + outputPorts + "]";
  }

}
