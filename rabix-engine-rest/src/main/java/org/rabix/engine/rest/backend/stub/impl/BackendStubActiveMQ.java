package org.rabix.engine.rest.backend.stub.impl;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.configuration.Configuration;
import org.rabix.bindings.model.Job;
import org.rabix.engine.rest.backend.HeartbeatInfo;
import org.rabix.engine.rest.backend.stub.BackendStub;
import org.rabix.engine.rest.service.JobService;
import org.rabix.engine.rest.service.JobServiceException;
import org.rabix.transport.backend.Backend;
import org.rabix.transport.backend.impl.BackendActiveMQ;
import org.rabix.transport.mechanism.TransportPlugin.ReceiveCallback;
import org.rabix.transport.mechanism.TransportPlugin.ResultPair;
import org.rabix.transport.mechanism.TransportPluginException;
import org.rabix.transport.mechanism.impl.activemq.TransportPluginActiveMQ;
import org.rabix.transport.mechanism.impl.activemq.TransportQueueActiveMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendStubActiveMQ implements BackendStub {

  private final static Logger logger = LoggerFactory.getLogger(BackendStubActiveMQ.class);

  private JobService jobService;
  private BackendActiveMQ backendActiveMQ;
  private TransportPluginActiveMQ transportPluginMQ;

  private TransportQueueActiveMQ sendToBackendQueue;
  private TransportQueueActiveMQ receiveFromBackendQueue;
  private TransportQueueActiveMQ receiveFromBackendHeartbeatQueue;

  private ExecutorService executorService = Executors.newFixedThreadPool(2);

  public BackendStubActiveMQ(JobService jobService, Configuration configuration, BackendActiveMQ backend) throws TransportPluginException {
    this.backendActiveMQ = backend;
    this.jobService = jobService;
    this.transportPluginMQ = new TransportPluginActiveMQ(configuration);

    this.sendToBackendQueue = new TransportQueueActiveMQ(backend.getToBackendQueue());
    this.receiveFromBackendQueue = new TransportQueueActiveMQ(backend.getFromBackendQueue());
    this.receiveFromBackendHeartbeatQueue = new TransportQueueActiveMQ(backend.getFromBackendHeartbeatQueue());
  }

  @Override
  public void start(final Map<String, Long> heartbeatInfo) {
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        while (true) {
          ResultPair<Job> result = transportPluginMQ.receive(receiveFromBackendQueue, Job.class, new ReceiveCallback<Job>() {
            @Override
            public void handleReceive(Job job) throws TransportPluginException {
              try {
                jobService.update(job);
              } catch (JobServiceException e) {
                throw new TransportPluginException("Failed to update Job", e);
              }
            }
          });
          if (!result.isSuccess()) {
            logger.error(result.getMessage(), result.getException());
          }
        }
      }
    });
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        transportPluginMQ.receive(receiveFromBackendHeartbeatQueue, HeartbeatInfo.class, new ReceiveCallback<HeartbeatInfo>() {
          @Override
          public void handleReceive(HeartbeatInfo entity) throws TransportPluginException {
            heartbeatInfo.put(entity.getId(), entity.getTimestamp());
          }
        });
      }
    });
  }

  @Override
  public void stop() {
    executorService.shutdown();
  }

  @Override
  public void send(Job job) {
    this.transportPluginMQ.send(sendToBackendQueue, job);
  }

  @Override
  public Backend getBackend() {
    return backendActiveMQ;
  }

}
