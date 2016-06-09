package org.rabix.bindings.protocol.draft3.processor.callback;

import java.util.List;
import java.util.Map;

import org.rabix.bindings.filemapper.FileMapper;
import org.rabix.bindings.model.ApplicationPort;
import org.rabix.bindings.protocol.draft3.helper.Draft3FileValueHelper;
import org.rabix.bindings.protocol.draft3.helper.Draft3SchemaHelper;
import org.rabix.bindings.protocol.draft3.processor.Draft3PortProcessorCallback;
import org.rabix.bindings.protocol.draft3.processor.Draft3PortProcessorException;
import org.rabix.bindings.protocol.draft3.processor.Draft3PortProcessorResult;
import org.rabix.common.helper.CloneHelper;

public class Draft3FilePathMapProcessorCallback implements Draft3PortProcessorCallback {

  private final FileMapper filePathMapper;

  public Draft3FilePathMapProcessorCallback(FileMapper filePathMapper) {
    this.filePathMapper = filePathMapper;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Draft3PortProcessorResult process(Object value, ApplicationPort port) throws Draft3PortProcessorException {
    if (value == null) {
      return new Draft3PortProcessorResult(value, false);
    }
    try {
      Object clonedValue = CloneHelper.deepCopy(value);
      
      if (Draft3SchemaHelper.isFileFromValue(clonedValue)) {
        Map<String, Object> valueMap = (Map<String, Object>) clonedValue;
        String path = Draft3FileValueHelper.getPath(valueMap);

        if (path != null && filePathMapper != null) {
          Draft3FileValueHelper.setPath(filePathMapper.map(path), valueMap);

          List<Map<String, Object>> secondaryFiles = Draft3FileValueHelper.getSecondaryFiles(valueMap);

          if (secondaryFiles != null) {
            for (Map<String, Object> secondaryFile : secondaryFiles) {
              String secondaryFilePath = Draft3FileValueHelper.getPath(secondaryFile);
              Draft3FileValueHelper.setPath(filePathMapper.map(secondaryFilePath), secondaryFile);
            }
          }
          return new Draft3PortProcessorResult(valueMap, true);
        }
      }
      return new Draft3PortProcessorResult(clonedValue, false);
    } catch (Exception e) {
      throw new Draft3PortProcessorException(e);
    }
    
  }

}
