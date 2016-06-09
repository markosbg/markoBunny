package org.rabix.bindings.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ApplicationPort {

  public static final String KEY_SCHEMA = "type";

  @JsonProperty("id")
  protected String id;
  
  @JsonProperty("default")
  protected Object defaultValue;
  
  @JsonProperty("type")
  protected Object schema;
  
  @JsonProperty("scatter")
  protected Boolean scatter;
  
  @JsonProperty("linkMerge")
  protected String linkMerge;
  
  protected Map<String, Object> raw = new HashMap<>();

  @JsonCreator
  public ApplicationPort(@JsonProperty("id") String id, @JsonProperty("default") Object defaultValue, @JsonProperty("type") Object schema, @JsonProperty("scatter") Boolean scatter, @JsonProperty("linkMerge") String linkMerge) {
    this.id = id;
    this.schema = schema;
    this.scatter = scatter;
    this.linkMerge = linkMerge;
    this.defaultValue = defaultValue;
  }

  @JsonAnySetter
  public void add(String key, Object value) {
    raw.put(key, value);
  }

  @JsonAnyGetter
  public Map<String, Object> getRaw() {
    return raw;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public Object getSchema() {
    return schema;
  }

  public Boolean getScatter() {
    return scatter;
  }

  public void setScatter(Boolean scatter) {
    this.scatter = scatter;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }
  
  public String getLinkMerge() {
    return linkMerge;
  }
  
  public void setLinkMerge(String linkMerge) {
    this.linkMerge = linkMerge;
  }
  
}
