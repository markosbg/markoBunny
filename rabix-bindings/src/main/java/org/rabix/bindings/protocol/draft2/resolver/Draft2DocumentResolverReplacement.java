package org.rabix.bindings.protocol.draft2.resolver;

import com.fasterxml.jackson.databind.JsonNode;

public class Draft2DocumentResolverReplacement {

  private final JsonNode parentNode;
  private final JsonNode referenceNode;
  private final String normalizedReferencePath;
  
  public Draft2DocumentResolverReplacement(JsonNode parentNode, JsonNode referenceNode, String normalizedReferencePath) {
    super();
    this.parentNode = parentNode;
    this.referenceNode = referenceNode;
    this.normalizedReferencePath = normalizedReferencePath;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((normalizedReferencePath == null) ? 0 : normalizedReferencePath.hashCode());
    result = prime * result + ((parentNode == null) ? 0 : parentNode.hashCode());
    result = prime * result + ((referenceNode == null) ? 0 : referenceNode.hashCode());
    return result;
  }

  public String getNormalizedReferencePath() {
    return normalizedReferencePath;
  }
  
  public JsonNode getParentNode() {
    return parentNode;
  }
  
  public JsonNode getReferenceNode() {
    return referenceNode;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Draft2DocumentResolverReplacement other = (Draft2DocumentResolverReplacement) obj;
    if (normalizedReferencePath == null) {
      if (other.normalizedReferencePath != null)
        return false;
    } else if (!normalizedReferencePath.equals(other.normalizedReferencePath))
      return false;
    if (parentNode == null) {
      if (other.parentNode != null)
        return false;
    } else if (!parentNode.equals(other.parentNode))
      return false;
    if (referenceNode == null) {
      if (other.referenceNode != null)
        return false;
    } else if (!referenceNode.equals(other.referenceNode))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Draft2DocumentResolverReplacement [normalizedReferencePath=" + normalizedReferencePath + ", parentNode=" + parentNode
        + ", referenceNode=" + referenceNode + "]";
  }

}
