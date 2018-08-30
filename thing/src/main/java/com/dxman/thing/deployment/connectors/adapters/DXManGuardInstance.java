package com.dxman.thing.deployment.connectors.adapters;

import com.dxman.execution.common.DXManWfNodeMapper;
import com.dxman.design.connectors.composition.*;
import com.dxman.execution.common.DXManWfCondition;
import com.dxman.execution.guard.DXManWfGuard;
import com.dxman.execution.guard.DXManWfGuardCustom;
import com.dxman.thing.deployment.connectors.common.DXManConnectorDataManager;
import com.dxman.thing.deployment.connectors.common.DXManConnectorInstance;
import com.dxman.thing.server.base.DXManConnectorRequester;
import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Damian Arellanes
 */
public class DXManGuardInstance extends DXManConnectorInstance {
  
  private class ConcurrentManager {
    
    public ConcurrentManager() {}

    public void sync() {
      ExecutorService executor = executeParallelChilds();
      
      if(executor != null)
        joinParallelChilds(executor);
    }

    public void async() {
      executeParallelChilds();
    }

    private ExecutorService executeParallelChilds() {

      // Adds all the tasks into a set
      Set<Callable<String>> tasks = new HashSet<>();
      
      for(DXManWfNodeMapper subNodeMapper: subNodeMappers) {
        
        DXManWfCondition condition = ((DXManWfGuardCustom)subNodeMapper
          .getCustom()).getCondition();
        
        if(connectorDataManager.matches(workflowId, 
          workflowTimestamp, condition)) {
          
          tasks.add((Callable) () -> {          
            transferControl(
              subNodeMapper.getNode(), subNodeMapper.getNode().getUri()
            );
            return null;
          });
        }
      }
      
      ExecutorService executor = null;
      if(tasks.size() > 0) {
        // Pool size = sum of tasks in the subnodes of the workflow tree
        executor = Executors.newFixedThreadPool(tasks.size());

        // Invokes all tasks in parallel
        try {
          executor.invokeAll(tasks);
        } catch (InterruptedException ex) {
          System.err.println(ex);
        }
        
        // Terminates threads for the tasks
        executor.shutdown();        
      }
      
      return executor;
    }

    private void joinParallelChilds(ExecutorService executor) {
      while (!executor.isTerminated()) {}
    }
  }
  
  private interface Invocation {
    public void execute();
  }
        
  private class InvocationContext {

    private Invocation invocation;

    public InvocationContext() {}

    public void execute() {
      invocation.execute();
    }

    public void setInvocation(DXManParallelType invocationType) {

      if(invocationType.equals(DXManParallelType.SYNC)) {
        invocation = new SynchronousInvocation();
      } else invocation = new AsynchronousInvocation();      
    }
  }
    
  private class AsynchronousInvocation implements Invocation {

    private final ConcurrentManager concurrentManager;

    public AsynchronousInvocation() {
      this.concurrentManager = new ConcurrentManager();
    }

    @Override
    public void execute() {
      concurrentManager.async();
    }
  }

  private class SynchronousInvocation implements Invocation {

    private final ConcurrentManager concurrentManager;

    public SynchronousInvocation() {
      this.concurrentManager = new ConcurrentManager();
    }

    @Override
    public void execute() {
      concurrentManager.sync();
    }
  }
  
  private final InvocationContext invocationContext;
  private List<DXManWfNodeMapper> subNodeMappers;
  private final DXManConnectorDataManager connectorDataManager;
  private String workflowId;
  private String workflowTimestamp;    

  public DXManGuardInstance(String connectorId, String connectorName, 
    DXManConnectorDataManager connectorDataManager, 
    DXManConnectorRequester requester, Gson gson) {
    
    super(connectorId, connectorName, requester, gson);
    
    this.connectorDataManager = connectorDataManager;
    
    invocationContext = new InvocationContext();    
    invocationContext.setInvocation(DXManParallelType.SYNC);
  }

  @Override
  public void activate(String workflowJSON) {
    
    System.err.println(getName() 
      + " (Guard Connector) activated [" + new Date() + "]");
    
    DXManWfGuard flow = gson.fromJson(workflowJSON, DXManWfGuard.class);
    subNodeMappers = flow.getSubnodeMappers();
    workflowId = flow.getWorkflowId();
    workflowTimestamp = flow.getWorkflowTimestamp();
    
    invocationContext.execute();
  }
}
