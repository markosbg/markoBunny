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
import org.rabix.transport.backend.impl.BackendRabbitMQ;
import org.rabix.transport.backend.impl.BackendRabbitMQ.BackendConfiguration;
import org.rabix.transport.backend.impl.BackendRabbitMQ.EngineConfiguration;
import org.rabix.transport.mechanism.TransportPlugin.ReceiveCallback;
import org.rabix.transport.mechanism.TransportPlugin.ResultPair;
import org.rabix.transport.mechanism.TransportPluginException;
import org.rabix.transport.mechanism.impl.rabbitmq.TransportPluginRabbitMQ;
import org.rabix.transport.mechanism.impl.rabbitmq.TransportQueueRabbitMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendStubRabbitMQ implements BackendStub {

  private final static Logger logger = LoggerFactory.getLogger(BackendStubActiveMQ.class);

  private JobService jobService;
  private BackendRabbitMQ backendRabbitMQ;
  private TransportPluginRabbitMQ transportPluginMQ;

  private TransportQueueRabbitMQ sendToBackendQueue;
  private TransportQueueRabbitMQ receiveFromBackendQueue;
  private TransportQueueRabbitMQ receiveFromBackendHeartbeatQueue;
  
  private final ExecutorService executorService = Executors.newFixedThreadPool(2);

  public BackendStubRabbitMQ(JobService jobService, BackendRabbitMQ backend, Configuration configuration) throws TransportPluginException {
    this.jobService = jobService;
    this.backendRabbitMQ = backend;

    this.transportPluginMQ = new TransportPluginRabbitMQ(configuration);

    BackendConfiguration backendConfiguration = backend.getBackendConfiguration();
    this.sendToBackendQueue = new TransportQueueRabbitMQ(backendConfiguration.getExchange(), backendConfiguration.getExchangeType(), backendConfiguration.getReceiveRoutingKey());

    EngineConfiguration engineConfiguration = backend.getEngineConfiguration();
    this.receiveFromBackendQueue = new TransportQueueRabbitMQ(engineConfiguration.getExchange(), engineConfiguration.getExchangeType(), engineConfiguration.getReceiveRoutingKey());
    this.receiveFromBackendHeartbeatQueue = new TransportQueueRabbitMQ(engineConfiguration.getExchange(), engineConfiguration.getExchangeType(), engineConfiguration.getHeartbeatRoutingKey());
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
    return backendRabbitMQ;
  }

}
