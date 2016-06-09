package org.rabix.bindings.protocol.draft3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rabix.bindings.BindingException;
import org.rabix.bindings.ProtocolRequirementProvider;
import org.rabix.bindings.model.FileValue;
import org.rabix.bindings.model.Job;
import org.rabix.bindings.model.requirement.DockerContainerRequirement;
import org.rabix.bindings.model.requirement.EnvironmentVariableRequirement;
import org.rabix.bindings.model.requirement.FileRequirement;
import org.rabix.bindings.model.requirement.FileRequirement.SingleFileRequirement;
import org.rabix.bindings.model.requirement.Requirement;
import org.rabix.bindings.protocol.draft3.bean.Draft3Job;
import org.rabix.bindings.protocol.draft3.bean.Draft3JobApp;
import org.rabix.bindings.protocol.draft3.bean.resource.Draft3Resource;
import org.rabix.bindings.protocol.draft3.bean.resource.requirement.Draft3CreateFileRequirement;
import org.rabix.bindings.protocol.draft3.bean.resource.requirement.Draft3DockerResource;
import org.rabix.bindings.protocol.draft3.bean.resource.requirement.Draft3EnvVarRequirement;
import org.rabix.bindings.protocol.draft3.bean.resource.requirement.Draft3EnvVarRequirement.EnvironmentDef;
import org.rabix.bindings.protocol.draft3.expression.Draft3ExpressionException;
import org.rabix.bindings.protocol.draft3.expression.Draft3ExpressionResolver;
import org.rabix.bindings.protocol.draft3.helper.Draft3FileValueHelper;
import org.rabix.bindings.protocol.draft3.helper.Draft3JobHelper;
import org.rabix.bindings.protocol.draft3.helper.Draft3SchemaHelper;

public class Draft3RequirementProvider implements ProtocolRequirementProvider {

  private DockerContainerRequirement getDockerRequirement(Draft3DockerResource draft2DockerResource) {
    if (draft2DockerResource == null) {
      return null;
    }
    return new DockerContainerRequirement(draft2DockerResource.getDockerPull(), draft2DockerResource.getImageId());
  }

  private EnvironmentVariableRequirement getEnvironmentVariableRequirement(Draft3Job draft3Job,
      Draft3EnvVarRequirement envVarRequirement) throws BindingException {
    if (envVarRequirement == null) {
      return null;
    }

    List<EnvironmentDef> envDefinitions = envVarRequirement.getEnvironmentDefinitions();
    if (envDefinitions == null) {
      return new EnvironmentVariableRequirement(Collections.<String, String> emptyMap());
    }
    Map<String, String> result = new HashMap<>();
    for (EnvironmentDef envDef : envDefinitions) {
      String key = envDef.getName();
      Object value = envDef.getValue();

      try {
        value = Draft3ExpressionResolver.resolve(value, draft3Job, null);
      } catch (Draft3ExpressionException e) {
        throw new BindingException(e);
      }
      if (value == null) {
        throw new BindingException("Environment variable for " + key + " is empty.");
      }
      result.put(key, value.toString());
    }
    return new EnvironmentVariableRequirement(result);

  }

  private FileRequirement getFileRequirement(Draft3Job draft3Job, Draft3CreateFileRequirement createFileRequirement)
      throws BindingException {
    if (createFileRequirement == null) {
      return null;
    }

    List<Draft3CreateFileRequirement.Draft3FileRequirement> fileRequirements = createFileRequirement
        .getFileRequirements();
    if (fileRequirements == null) {
      return null;
    }

    List<SingleFileRequirement> result = new ArrayList<>();
    for (Draft3CreateFileRequirement.Draft3FileRequirement fileRequirement : fileRequirements) {
      try {
        String filename = (String) fileRequirement.getFilename(draft3Job);

        Object content = fileRequirement.getContent(draft3Job);

        if (Draft3SchemaHelper.isFileFromValue(content)) {
          FileValue fileValue = Draft3FileValueHelper.createFileValue(content);
          result.add(new FileRequirement.SingleInputFileRequirement(filename, fileValue));
        } else {
          result.add(new FileRequirement.SingleTextFileRequirement(filename, (String) content));
        }
      } catch (Draft3ExpressionException e) {
        throw new BindingException(e);
      }
    }
    return new FileRequirement(result);
  }

  @Override
  public List<Requirement> getRequirements(Job job) throws BindingException {
    Draft3Job draft2Job = Draft3JobHelper.getDraft3Job(job);
    Draft3JobApp draft2JobApp = draft2Job.getApp();
    return convertRequirements(job, draft2JobApp.getRequirements());
  }

  @Override
  public List<Requirement> getHints(Job job) throws BindingException {
    Draft3Job draft2Job = Draft3JobHelper.getDraft3Job(job);
    Draft3JobApp draft2JobApp = draft2Job.getApp();
    return convertRequirements(job, draft2JobApp.getHints());
  }

  private List<Requirement> convertRequirements(Job job, List<Draft3Resource> resources) throws BindingException {
    if (resources == null) {
      return Collections.<Requirement> emptyList();
    }
    Draft3Job draft2Job = Draft3JobHelper.getDraft3Job(job);

    List<Requirement> result = new ArrayList<>();
    for (Draft3Resource draft2Resource : resources) {
      if (draft2Resource instanceof Draft3DockerResource) {
        result.add(getDockerRequirement((Draft3DockerResource) draft2Resource));
        continue;
      }
      if (draft2Resource instanceof Draft3EnvVarRequirement) {
        result.add(getEnvironmentVariableRequirement(draft2Job, (Draft3EnvVarRequirement) draft2Resource));
        continue;
      }
      if (draft2Resource instanceof Draft3CreateFileRequirement) {
        result.add(getFileRequirement(draft2Job, (Draft3CreateFileRequirement) draft2Resource));
        continue;
      }
    }
    return result;
  }

}
