package org.rabix.executor.handler;

import org.rabix.bindings.model.Job;
import org.rabix.executor.ExecutorException;
import org.rabix.executor.engine.EngineStub;

public interface JobHandler {

  public final static int DEFAULT_ERROR_CODE = -1;
  public final static int DEFAULT_SUCCESS_CODE = 0;

  /**
   * Start execution
   */
  void start() throws ExecutorException;

  /**
   * Stop execution
   */
  void stop() throws ExecutorException;

  /**
   * Is handler started 
   */
  boolean isStarted() throws ExecutorException;
  
  /**
   * Is handler running
   */
  boolean isRunning() throws ExecutorException;

  /**
   * Get exit status
   */
  int getExitStatus() throws ExecutorException;

  /**
   * Do after-processing
   */
  Job postprocess(boolean isTerminal) throws ExecutorException;

  /**
   * Is Job finished successfully or not
   */
  boolean isSuccessful(int processExitCode) throws ExecutorException;
  
  /**
   * Is Job finished successfully or not
   */
  boolean isSuccessful() throws ExecutorException;

  EngineStub getEngineStub();
  
}
