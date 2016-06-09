package org.rabix.executor.container.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.rabix.bindings.Bindings;
import org.rabix.bindings.BindingsFactory;
import org.rabix.bindings.model.Job;
import org.rabix.bindings.model.requirement.EnvironmentVariableRequirement;
import org.rabix.bindings.model.requirement.Requirement;
import org.rabix.executor.config.StorageConfig;
import org.rabix.executor.container.ContainerException;
import org.rabix.executor.container.ContainerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalContainerHandler implements ContainerHandler {

  private final static Logger logger = LoggerFactory.getLogger(LocalContainerHandler.class);

  private Job job;
  private File workingDir;

  private Future<Integer> processFuture;
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  private Process process;

  public LocalContainerHandler(Job job, Configuration configuration) {
    this.job = job;
    this.workingDir = StorageConfig.getWorkingDir(job, configuration);
  }

  @Override
  public synchronized void start() throws ContainerException {
    try {
      Bindings bindings = BindingsFactory.create(job);
      String commandLine = bindings.buildCommandLine(job);

      File commandLineFile = new File(workingDir, "cmd.log");
      FileUtils.writeStringToFile(commandLineFile, commandLine);

      final ProcessBuilder processBuilder = new ProcessBuilder();
      List<Requirement> combinedRequirements = new ArrayList<>();
      combinedRequirements.addAll(bindings.getHints(job));
      combinedRequirements.addAll(bindings.getRequirements(job));

      EnvironmentVariableRequirement environmentVariableResource = getRequirement(combinedRequirements, EnvironmentVariableRequirement.class);
      if (environmentVariableResource != null) {
        Map<String, String> env = processBuilder.environment();
        for (Entry<String, String> envVariableEntry : environmentVariableResource.getVariables().entrySet()) {
          env.put(envVariableEntry.getKey(), envVariableEntry.getValue());
        }
      }

      processBuilder.command("/bin/sh", "-c", commandLine);
      processBuilder.directory(workingDir);
      processFuture = executorService.submit(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
          process = processBuilder.start();
          process.waitFor();
          return process.exitValue();
        }
      });
      logger.info("Local container has started.");
    } catch (Exception e) {
      logger.error("Failed to start application", e);
      throw new ContainerException("Failed to start application", e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Requirement> T getRequirement(List<Requirement> requirements, Class<T> clazz) {
    for (Requirement requirement : requirements) {
      if (requirement.getClass().equals(clazz)) {
        return (T) requirement;
      }
    }
    return null;
  }

  @Override
  public synchronized void stop() throws ContainerException {
    if (processFuture == null) {
      return;
    }
    processFuture.cancel(true);
  }

  @Override
  public synchronized boolean isStarted() throws ContainerException {
    return processFuture != null;
  }

  @Override
  public synchronized boolean isRunning() throws ContainerException {
    if (processFuture == null) {
      return false;
    }
    return !processFuture.isDone();
  }

  @Override
  public synchronized int getProcessExitStatus() throws ContainerException {
    try {
      return processFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new ContainerException(e);
    }
  }

  @Override
  public synchronized void dumpContainerLogs(File errorFile) throws ContainerException {
    try {
      if (!errorFile.exists()) {
        errorFile.createNewFile();
      }
      try (InputStream inputStream = process.getErrorStream(); OutputStream outputStream = new FileOutputStream(errorFile)) {
        IOUtils.copy(inputStream, outputStream);
      }
    } catch (IOException e) {
      logger.error("Failed to create " + errorFile.getName(), e);
      throw new ContainerException("Failed to create " + errorFile.getName(), e);
    }
  }
}
