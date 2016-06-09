package org.rabix.bindings.protocol.draft2.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.rabix.bindings.protocol.draft2.bean.Draft2Job;
import org.rabix.bindings.protocol.draft2.bean.Draft2OutputPort;
import org.rabix.bindings.protocol.draft2.expression.Draft2ExpressionException;
import org.rabix.bindings.protocol.draft2.expression.helper.Draft2ExpressionBeanHelper;
import org.rabix.bindings.protocol.draft2.helper.Draft2BindingHelper;
import org.rabix.bindings.protocol.draft2.helper.Draft2FileValueHelper;
import org.rabix.bindings.protocol.draft2.helper.Draft2SchemaHelper;
import org.rabix.bindings.protocol.draft2.service.Draft2MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Draft2MetadataServiceImpl implements Draft2MetadataService {

  private final static Logger logger = LoggerFactory.getLogger(Draft2MetadataServiceImpl.class);
  
  /**
   * Process metadata inheritance
   */
  public Map<String, Object> processMetadata(Draft2Job job, Object value, Draft2OutputPort outputPort, Object outputBinding) {
    if (outputPort.getOutputBinding() != null) {
      outputBinding = outputPort.getOutputBinding(); // override
    }
    
    Map<String, Object> metadata = Draft2FileValueHelper.getMetadata(value);

    String inputId = Draft2BindingHelper.getInheritMetadataFrom(outputBinding);
    if (StringUtils.isEmpty(inputId)) {
      logger.info("Metadata for {} is {}.", outputPort.getId(), metadata);
      return metadata;
    }

    Object input = null;
    String normalizedInputId = Draft2SchemaHelper.normalizeId(inputId);
    for (Entry<String, Object> inputEntry : job.getInputs().entrySet()) {
      if (inputEntry.getKey().equals(normalizedInputId)) {
        input = inputEntry.getValue();
        break;
      }
    }

    List<Map<String, Object>> metadataList = findAllMetadata(input);
    Map<String, Object> inheritedMetadata = intersect(metadataList);
    if (inheritedMetadata == null) {
      return metadata;
    }

    if (metadata != null) {
      inheritedMetadata.putAll(metadata);
    }
    logger.info("Metadata for {} is {}.", outputPort.getId(), inheritedMetadata);
    return inheritedMetadata;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> findAllMetadata(Object input) {
    if (input == null) {
      return null;
    }
    List<Map<String, Object>> result = new ArrayList<>();

    if (Draft2SchemaHelper.isFileFromValue(input)) {
      Map<String, Object> metadata = Draft2FileValueHelper.getMetadata(input);
      result.add(metadata != null ? metadata : new HashMap<String, Object>());
    } else if (input instanceof List<?>) {
      for (Object inputPart : ((List<Object>) input)) {
        List<Map<String, Object>> resultPart = findAllMetadata(inputPart);
        if (resultPart != null) {
          result.addAll(resultPart);
        }
      }
    } else if (input instanceof Map<?, ?>) {
      for (Entry<String, Object> inputPartEntry : ((Map<String, Object>) input).entrySet()) {
        List<Map<String, Object>> resultPart = findAllMetadata(inputPartEntry.getValue());
        if (resultPart != null) {
          result.addAll(resultPart);
        }
      }
    }
    return result;
  }

  private Map<String, Object> intersect(List<Map<String, Object>> metadataList) {
    if (metadataList == null || metadataList.isEmpty()) {
      return null;
    }

    Map<String, Object> inheritedMeta = new HashMap<String, Object>();

    Map<String, Object> firstMetaData = metadataList.get(0);
    for (String metaDataKey : firstMetaData.keySet()) {
      Object metaDataValue1 = firstMetaData.get(metaDataKey);

      boolean equals = true;
      for (int i = 1; i < metadataList.size(); i++) {
        Object metaDataValue2 = metadataList.get(i).get(metaDataKey);

        if (!(
            (metaDataValue1 == null && metaDataValue2 == null) // special case (preserve null "value")
            || (metaDataValue1 != null && metaDataValue2 != null && metaDataValue1.equals(metaDataValue2))) // values are the same
            ) {
          equals = false;
          break;
        }

      }
      if (equals) {
        inheritedMeta.put(metaDataKey, metaDataValue1);
      }
    }
    return inheritedMeta;
  }

  @SuppressWarnings("unchecked")
  public Object evaluateMetadataExpressions(Draft2Job job, Object self, Object metadata) throws Draft2ExpressionException {
    if (metadata == null) {
      return null;
    }
    Object result = metadata;
    if (Draft2ExpressionBeanHelper.isExpression(metadata)) {
      result = Draft2ExpressionBeanHelper.evaluate(job, self, metadata);
    } else if (metadata instanceof Map<?, ?>) {
      result = new HashMap<>();
      for (Entry<String, Object> outputEntry : ((Map<String, Object>) metadata).entrySet()) {
        Object resolved = evaluateMetadataExpressions(job, self, outputEntry.getValue());
        ((Map<String, Object>) result).put(outputEntry.getKey(), resolved);
      }
      return result;
    } else if (metadata instanceof List<?>) {
      result = new ArrayList<>();
      for (Object value : ((List<Object>) metadata)) {
        Object resolvedMetadata = evaluateMetadataExpressions(job, self, value);
        if (resolvedMetadata != null) {
          ((List<Object>) result).add(resolvedMetadata);
        }
      }
    }
    return result;
  }

}
