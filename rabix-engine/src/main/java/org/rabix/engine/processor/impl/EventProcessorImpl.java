package org.rabix.engine.processor.impl;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.rabix.engine.event.Event;
import org.rabix.engine.event.Event.EventType;
import org.rabix.engine.event.impl.ContextStatusEvent;
import org.rabix.engine.model.ContextRecord;
import org.rabix.engine.model.ContextRecord.ContextStatus;
import org.rabix.engine.processor.EventProcessor;
import org.rabix.engine.processor.dispatcher.EventDispatcher;
import org.rabix.engine.processor.dispatcher.EventDispatcherFactory;
import org.rabix.engine.processor.handler.EventHandlerException;
import org.rabix.engine.processor.handler.HandlerFactory;
import org.rabix.engine.service.ContextRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Event processor implementation
 */
public class EventProcessorImpl implements EventProcessor {

  private static final Logger logger = LoggerFactory.getLogger(EventProcessorImpl.class);
  
  public final static long SLEEP = 100;
  
  private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final AtomicBoolean stop = new AtomicBoolean(false);
  private final AtomicBoolean running = new AtomicBoolean(false);

  private final HandlerFactory handlerFactory;
  private final EventDispatcher eventDispatcher;
  
  private final ContextRecordService contextRecordService;
  
  private final ConcurrentMap<String, Integer> iterations = new ConcurrentHashMap<>();
  
  @Inject
  public EventProcessorImpl(HandlerFactory handlerFactory, EventDispatcherFactory eventDispatcherFactory, ContextRecordService contextRecordService) {
    this.handlerFactory = handlerFactory;
    this.contextRecordService = contextRecordService;
    this.eventDispatcher = eventDispatcherFactory.create(EventDispatcher.Type.SYNC);
  }

  public void start(final List<IterationCallback> iterationCallbacks) {
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        Event event = null;
        while (!stop.get()) {
          try {
            event = events.poll();
            if (event == null) {
              running.set(false);
              Thread.sleep(SLEEP);
              continue;
            }
            ContextRecord context = contextRecordService.find(event.getContextId());
            if (context != null && context.getStatus().equals(ContextStatus.FAILED)) {
              logger.info("Skip event {}. Context {} has been invalidated.", event, context.getId());
              continue;
            }
            running.set(true);
            handlerFactory.get(event.getType()).handle(event);

            Integer iteration = iterations.get(event.getContextId());
            if (iteration == null) {
              iteration = 0;
            }
            
            iteration++;
            if (iterationCallbacks != null) {
              for (IterationCallback callback : iterationCallbacks) {
                callback.call(EventProcessorImpl.this, event.getContextId(), iteration);
              }
            }
            iterations.put(event.getContextId(), iteration);
          } catch (Exception e) {
            logger.error("EventProcessor failed to process event {}.", event, e);
            try {
              invalidateContext(event.getContextId());
            } catch (EventHandlerException ehe) {
              logger.error("Failed to invalidate Context {}.", event.getContextId(), ehe);
              stop();
            }
          }
        }
      }
    });
  }
  
  /**
   * Invalidates context 
   */
  private void invalidateContext(String contextId) throws EventHandlerException {
    handlerFactory.get(Event.EventType.CONTEXT_STATUS_UPDATE).handle(new ContextStatusEvent(contextId, ContextStatus.FAILED));
  }
  
  @Override
  public void stop() {
    stop.set(true);
    running.set(false);
  }

  public boolean isRunning() {
    return running.get();
  }

  public void send(Event event) throws EventHandlerException {
    if (stop.get()) {
      return;
    }
    if (event.getType().equals(EventType.INIT)) {
      addToQueue(event);
      return;
    }
    eventDispatcher.send(event);
  }

  public void addToQueue(Event event) {
    if (stop.get()) {
      return;
    }
    this.events.add(event);
  }

}
