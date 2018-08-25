package com.dxman.deployment.cli;

import com.dxman.dataspace.base.*;
import com.dxman.deployment.data.DXManDataAlgorithm;
import com.dxman.design.data.*;
import com.dxman.design.services.atomic.DXManAtomicServiceTemplate;
import com.dxman.design.services.common.*;
import com.dxman.design.services.composite.DXManCompositeServiceTemplate;
import com.dxman.execution.*;
import com.dxman.execution.selector.DXManWfSelector;
import com.dxman.execution.selector.DXManWfSelectorCustom;
import com.dxman.utils.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import org.eclipse.californium.core.CoapClient;

/**
 * @author Damian Arellanes
 */
public class DXManWorkflowTreeDeployer {
  
  private final DXManDataSpace dataSpace;
  private final Gson GSON;
  
  private StringBuilder COMPOSITE_CONTENT;
  private final String COMPOSITE_EXTENSION = ".comp";
  private final String WT_EXTENSION = ".wt";
  
  public DXManWorkflowTreeDeployer(String dataSpaceLocation) {    
    
    dataSpace = DXManDataSpaceFactory.createBlockchainManager(dataSpaceLocation);
    
    RuntimeTypeAdapterFactory<DXManServiceTemplate> adapter0 = RuntimeTypeAdapterFactory
      .of(DXManServiceTemplate.class, "classTypeServ")
      .registerSubtype(DXManCompositeServiceTemplate.class, DXManCompositeServiceTemplate.class.getName())
      .registerSubtype(DXManAtomicServiceTemplate.class, DXManAtomicServiceTemplate.class.getName());
    RuntimeTypeAdapterFactory<DXManWfNode> adapter1 = RuntimeTypeAdapterFactory
      .of(DXManWfNode.class, "classTypeWfNode")
      .registerSubtype(DXManWfParallel.class, DXManWfParallel.class.getName())
      .registerSubtype(DXManWfSelector.class, DXManWfSelector.class.getName())
      .registerSubtype(DXManWfSequencer.class, DXManWfSequencer.class.getName())
      .registerSubtype(DXManWfInvocation.class, DXManWfInvocation.class.getName());
    RuntimeTypeAdapterFactory<DXManWfNodeCustom> adapter2 = RuntimeTypeAdapterFactory
      .of(DXManWfNodeCustom.class, "classTypeWfNodeCustom")
      .registerSubtype(DXManWfParallelCustom.class, DXManWfParallelCustom.class.getName())
      .registerSubtype(DXManWfSelectorCustom.class, DXManWfSelectorCustom.class.getName())
      .registerSubtype(DXManWfSequencerCustom.class, DXManWfSequencerCustom.class.getName());
    
    GSON = new GsonBuilder().disableHtmlEscaping()
      .registerTypeAdapterFactory(adapter0)
      .registerTypeAdapterFactory(adapter1)
      .create();
  }
  
  public DXManWfResult executeWorkflow(DXManWorkflowTreeEditor wtEditor, 
    DXManWfNode node) {
        
    String workflowId = wtEditor.getWorkflowTree().getId();
    String workflowTimestamp = wtEditor.getWorkflowTree().getCreationTimestamp();
    
    // Writes input parameters
    System.out.println("Updating inputs in the blockchain...");
    List<DXManDataParameter> dp = new ArrayList<>();
    
    wtEditor.getInputs().forEach((paramId, paramValue)->{
      dp.add(new DXManDataParameter(paramId, workflowId, paramValue, new ArrayList<>()));
    });
    dataSpace.writeParameters(dp, workflowId);   
    
    RuntimeTypeAdapterFactory<DXManWfNode> adapter1 = RuntimeTypeAdapterFactory
      .of(DXManWfNode.class)
      .registerSubtype(DXManWfParallel.class)
      .registerSubtype(DXManWfSelector.class)
      .registerSubtype(DXManWfSequencer.class)
      .registerSubtype(DXManWfInvocation.class);
    RuntimeTypeAdapterFactory<DXManWfNodeCustom> adapter2 = RuntimeTypeAdapterFactory
      .of(DXManWfNodeCustom.class)
      .registerSubtype(DXManWfParallelCustom.class)
      .registerSubtype(DXManWfSelectorCustom.class)
      .registerSubtype(DXManWfSequencerCustom.class);
    Gson gson = new GsonBuilder().disableHtmlEscaping()
      .registerTypeAdapterFactory(adapter1)
      .registerTypeAdapterFactory(adapter2)
      .create();
    
    // Executes the workflow
    CoapClient cp = new CoapClient(node.getUri());
    cp.post(gson.toJson(node), 0);
    
    // Reads output parameters
    DXManWfResult outputValues = new DXManWfResult();
    for(String outputId: wtEditor.getOutputs()) {      
      outputValues.put(outputId, dataSpace.readParameter(outputId, workflowId, workflowTimestamp));
    }
    
    return outputValues;
  }
  
  public void deployWorkflow(DXManWorkflowTreeEditor wtEditor, 
    boolean deployDataChannels) {
    
    DXManDataAlgorithm alg = new DXManDataAlgorithm();
 
    wtEditor.design();
    
    wtEditor.getWorkflowTree().getWt().get(
      wtEditor.getWorkflowTree().getCompositeService().getId()
    ).deploy(alg, wtEditor.getWorkflowTree());
    
    if(deployDataChannels)
      deployWorkflowDataChannels(alg, wtEditor.getWorkflowTree().getId());
  }
  
  private void deployWorkflowDataChannels(DXManDataAlgorithm alg, String wfId) {
    
    /*System.out.println("Deploying data channels for workflow " + wfId + "...");
    
    alg.getReaders().forEach((rId, writers) ->{
      System.out.println("Deploying: " + rId + "--->" + writers);      
    });*/
    // TODO Optimize this (perhaps sending the whole readers to every WfNode of the WfTree)
    // So every WfNode can access the data pipes from there
    DXManMap<String, String> alreadyDeployed = new DXManMap<>();
    List<DXManDataParameter> parametersToDeploy = new ArrayList<>();
    alg.getWriters().forEach((writerId, readers) ->{
      
      //System.out.println("Deploying: " + writerId + "--->" + readers);      
      //System.out.println("Deploying: " + writerId);
      
      List<String> setReaders = new ArrayList<>();
      for(String reader: readers) {
      
        //System.out.println("Deploying: " + reader); 
        if(alreadyDeployed.get(reader) == null) {
      
          parametersToDeploy.add(new DXManDataParameter(
            reader, wfId, "null", new ArrayList<>())
          );
          //dataSpace.registerParameter(reader, wfId, "null", new ArrayList<>());
          alreadyDeployed.put(reader, reader);
        }          
        setReaders.add(reader);        
      }
      
      parametersToDeploy.add(new DXManDataParameter(
        writerId, wfId, "null", setReaders)
      );
      //dataSpace.registerParameter(writerId, wfId, "null", setReaders);
      alreadyDeployed.put(writerId, writerId);      
    });
    
    dataSpace.registerParameters(parametersToDeploy, wfId);
  }
  
  public DXManWorkflowTree readWorkflowTreeDescription(String fileName) {
    
    return GSON.fromJson(
      readFile(fileName + WT_EXTENSION), 
      DXManWorkflowTree.class
    );
  }
  
  public void buildWorkflowTree(String fileName, 
    DXManCompositeServiceTemplate composite) {
    
    DXManWorkflowTree wfTree = new DXManWorkflowTree(composite);        
    COMPOSITE_CONTENT = new StringBuilder();

    // Generates the workflow tree
    generateWorkflowTree(composite, wfTree);
    
    // Creates the json represenattion of the workflow tree
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();    
    writeFile(fileName + WT_EXTENSION, gson.toJson(wfTree));
    writeFile(fileName + COMPOSITE_EXTENSION, COMPOSITE_CONTENT.toString());
    
    System.out.println("The workflow tree for " + composite.getInfo().getName()
      + " has been created in the file " + fileName + WT_EXTENSION);
    System.out.println("The composite service template description for " 
      + composite.getInfo().getName() + " has been created in the file " 
      + fileName + COMPOSITE_EXTENSION);
  }
    
  private DXManWfNode generateWorkflowTree(
    DXManCompositeServiceTemplate composite, DXManWorkflowTree wt) {
    
    // Sets the id for the current service which is used as the resource for the server
    composite.setId(DXManIDGenerator.generateServiceID());
    
    DXManWfNode parentWfNode = createWfNodeInstance(
      composite, composite.getId(), ""
    );
    
    for(DXManServiceTemplate subService: 
      composite.getCompositionConnector().getSubServices()) {
      
      // Sets the id for the subservice which is used as the resource for the server
      subService.setId(DXManIDGenerator.generateServiceID());
      
      // Adds the operations to the workflow tree      
      subService.getOperations().forEach((opId, op)->{
        
        DXManWfNode opNode = createWfNodeInstance(subService, opId, opId);
        parentWfNode.addSubWfNode(opNode, new DXManWfNodeCustom() {});
        
        updateWorkflowTree(wt, opNode);
      });
      
      if(subService.getType().equals(DXManServiceType.COMPOSITE)) {
        
        DXManWfNode subWfNode = generateWorkflowTree(
          (DXManCompositeServiceTemplate) subService, wt
        );
        parentWfNode.addSubWfNode(subWfNode, new DXManWfNodeCustom() {});
      } else {
        COMPOSITE_CONTENT.append(subService);
      }
      
      generateAlgebraicDataChannels(composite, subService, parentWfNode);
    }
    
    updateWorkflowTree(wt, parentWfNode);
    
    COMPOSITE_CONTENT.append(composite);
    
    return parentWfNode;
  }
  
  private DXManWfNode createWfNodeInstance(DXManServiceTemplate service, 
    String wfNodeId, String operationName) {
    
    String uri = DXManIDGenerator.getCoapUri(
      service.getDeploymentInfo().getThingIp(), 
      service.getDeploymentInfo().getThingPort(), 
      service.getId()); // TODO The uri should be to the operation not the service
    
    if(service.getType().equals(DXManServiceType.ATOMIC)) {
      return new DXManWfInvocation(wfNodeId, uri, operationName);
    } else {
      
      switch(((DXManCompositeServiceTemplate) service).getCompositionConnector().getType()) {
      case SEQUENCER:
        return new DXManWfSequencer(wfNodeId, uri);
      case SELECTOR:
        return new DXManWfSelector(wfNodeId, uri);
    }
    }
    return null;    
  }
  
  private void updateWorkflowTree(DXManWorkflowTree workflowTree, DXManWfNode wfNode) {
    workflowTree.getWt().put(wfNode.getId(), wfNode);
  }
  
  private void generateAlgebraicDataChannels(
    DXManCompositeServiceTemplate composite, DXManServiceTemplate subService,
    DXManWfNode wfNode
  ) {
    
    // Adds all the operations of subservices to composite service
    subService.getOperations().forEach((opId, op)->{

      DXManOperation compositeOp = op.clone();
      composite.addOperation(compositeOp);

      op.getParameters().forEach((parName, par)->{
                
        DXManDataChannelPoint origin;
        DXManDataChannelPoint destination;
        if(par.getParameterType().equals(DXManParameterType.INPUT)) {

          origin = new DXManDataChannelPoint(
            compositeOp.getInputs().get(parName).getId()
          );            
          destination = new DXManDataChannelPoint(par.getId());
        } else {

          origin = new DXManDataChannelPoint(par.getId());
          destination = new DXManDataChannelPoint(
            compositeOp.getOutputs().get(parName).getId()
          );
        }

        DXManDataChannel dc = new DXManDataChannel(origin, destination);
        wfNode.getDataChannels().add(dc);
      });
    });
  }
  
  private String readFile(String fileName) {
    
    String json = "";
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(fileName));
      json = reader.readLine();
      reader.close();
    } catch (FileNotFoundException ex) { System.out.println(ex); } 
      catch (IOException ex) { System.out.println(ex); } 
    finally {
      try { reader.close(); } catch (IOException ex) { System.out.println(ex); }
    }
    
    return json;
  }
  
  private void writeFile(String fileName, String json) {
    
    BufferedWriter writer;
    try {
      
      writer = new BufferedWriter(new FileWriter(fileName, false));
      writer.append(json);

      writer.close();
    } catch (IOException ex) { System.out.println(ex.toString()); }
  }
}