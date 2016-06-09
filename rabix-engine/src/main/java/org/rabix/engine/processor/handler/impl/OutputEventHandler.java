package org.rabix.engine.processor.handler.impl;

import java.util.List;

import org.rabix.bindings.model.dag.DAGLinkPort.LinkPortType;
import org.rabix.common.helper.InternalSchemaHelper;
import org.rabix.engine.event.Event;
import org.rabix.engine.event.impl.ContextStatusEvent;
import org.rabix.engine.event.impl.InputUpdateEvent;
import org.rabix.engine.event.impl.OutputUpdateEvent;
import org.rabix.engine.model.ContextRecord.ContextStatus;
import org.rabix.engine.model.JobRecord;
import org.rabix.engine.model.LinkRecord;
import org.rabix.engine.model.VariableRecord;
import org.rabix.engine.model.scatter.ScatterStrategy;
import org.rabix.engine.processor.EventProcessor;
import org.rabix.engine.processor.handler.EventHandler;
import org.rabix.engine.processor.handler.EventHandlerException;
import org.rabix.engine.service.JobRecordService;
import org.rabix.engine.service.JobRecordService.JobState;
import org.rabix.engine.service.LinkRecordService;
import org.rabix.engine.service.VariableRecordService;

import com.google.inject.Inject;

/**
 * Handles {@link OutputUpdateEvent} events.
 */
public class OutputEventHandler implements EventHandler<OutputUpdateEvent> {

  private JobRecordService jobService;
  private VariableRecordService variableService;
  private LinkRecordService linkService;
  
  private final EventProcessor eventProcessor;
  
  @Inject
  public OutputEventHandler(EventProcessor eventProcessor, JobRecordService jobService, VariableRecordService variableService, LinkRecordService linkService) {
    this.jobService = jobService;
    this.linkService = linkService;
    this.variableService = variableService;
    this.eventProcessor = eventProcessor;
  }

  public void handle(final OutputUpdateEvent event) throws EventHandlerException {
    JobRecord sourceJob = jobService.find(event.getJobId(), event.getContextId());
    if (event.isFromScatter()) {
      sourceJob.resetOutputPortCounters(event.getNumberOfScattered());
    }
    VariableRecord sourceVariable = variableService.find(event.getJobId(), event.getPortId(), LinkPortType.OUTPUT, event.getContextId());
    sourceJob.decrementPortCounter(event.getPortId(), LinkPortType.OUTPUT);
    sourceVariable.addValue(event.getValue(), event.getPosition());
    jobService.update(sourceJob);
    
    if (sourceJob.isCompleted()) {
      sourceJob.setState(JobState.COMPLETED);
      jobService.update(sourceJob);
      if (sourceJob.isMaster()) {
        eventProcessor.addToQueue(new ContextStatusEvent(event.getContextId(), ContextStatus.COMPLETED));
      }
    }
    
    Object value = null;
    
    if (sourceJob.isScatterWrapper()) {
      ScatterStrategy scatterStrategy = sourceJob.getScatterStrategy();
      
      if (scatterStrategy.isBlocking()) {
        if (sourceJob.isOutputPortReady(event.getPortId())) {
          value = scatterStrategy.values(sourceJob.getId(), event.getPortId(), event.getContextId());
        } else {
          return;
        }
      }
      
      List<LinkRecord> links = linkService.findBySource(sourceVariable.getJobId(), sourceVariable.getPortId(), event.getContextId());
      for (LinkRecord link : links) {
        List<VariableRecord> destinationVariables = variableService.find(link.getDestinationJobId(), link.getDestinationJobPort(), event.getContextId());

        JobRecord destinationJob = null;
        boolean isDestinationPortScatterable = false;
        for (VariableRecord destinationVariable : destinationVariables) {
          switch (destinationVariable.getType()) {
          case INPUT:
            destinationJob = jobService.find(destinationVariable.getJobId(), destinationVariable.getContextId());
            isDestinationPortScatterable = destinationJob.isScatterPort(destinationVariable.getPortId());
            if (isDestinationPortScatterable && !destinationJob.isBlocking() && !(destinationJob.getInputPortIncoming(event.getPortId()) > 1)) {
              value = value != null ? value : event.getValue();
              int numberOfScattered = sourceJob.getNumberOfGlobalOutputs();
              Event updateInputEvent = new InputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, true, numberOfScattered, event.getPosition());
              eventProcessor.send(updateInputEvent);
            } else {
              if (sourceJob.isOutputPortReady(event.getPortId())) {
                value = value != null ? value : sourceVariable.getValue();
                Event updateInputEvent = new InputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, link.getPosition());
                eventProcessor.send(updateInputEvent);
              }
            }
            break;
          case OUTPUT:
            destinationJob = jobService.find(destinationVariable.getJobId(), destinationVariable.getContextId());
            if (!(destinationJob.getOutputPortIncoming(event.getPortId()) > 1)) {
              value = value != null? value : event.getValue();
              int numberOfScattered = sourceJob.getNumberOfGlobalOutputs();
              if (scatterStrategy.isBlocking()) {
                Event updateOutputEvent = new OutputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, false, 1, 1);
                eventProcessor.send(updateOutputEvent);
              } else {
                Event updateOutputEvent = new OutputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, true, numberOfScattered, event.getPosition());
                eventProcessor.send(updateOutputEvent);
              }
            } else {
              if (sourceJob.isOutputPortReady(event.getPortId())) {
                value = value != null? value : sourceVariable.getValue();
                Event updateInputEvent = new InputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, link.getPosition());
                eventProcessor.send(updateInputEvent);
              }
            }
            break;
          }
        }
      }
      return;
    }
    
    if (sourceJob.isOutputPortReady(event.getPortId())) {
      List<LinkRecord> links = linkService.findBySource(event.getJobId(), event.getPortId(), event.getContextId());
      for (LinkRecord link : links) {
        List<VariableRecord> destinationVariables = variableService.find(link.getDestinationJobId(), link.getDestinationJobPort(), event.getContextId());
        
        value = sourceVariable.getValue();
        for (VariableRecord destinationVariable : destinationVariables) {
          switch (destinationVariable.getType()) {
          case INPUT:
            Event updateInputEvent = new InputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, link.getPosition());
            eventProcessor.send(updateInputEvent);
            break;
          case OUTPUT:
            if (sourceJob.isScattered()) {
              int numberOfScattered = sourceJob.getNumberOfGlobalOutputs();
              int position = InternalSchemaHelper.getScatteredNumber(sourceJob.getId());
              Event updateOutputEvent = new OutputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, true, numberOfScattered, position);
              eventProcessor.send(updateOutputEvent);
            } else {
              Event updateOutputEvent = new OutputUpdateEvent(event.getContextId(), destinationVariable.getJobId(), destinationVariable.getPortId(), value, link.getPosition());
              eventProcessor.send(updateOutputEvent);
            }
            break;
          }
        }
      }
    }
  }
  
}
