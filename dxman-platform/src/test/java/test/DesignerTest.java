package test;

import com.dxman.deployment.cli.DXManWorkflowTreeDeployer;
import com.dxman.deployment.cli.DXManWorkflowTreeEditor;
import com.dxman.deployment.common.DXManDeploymentManager;
import com.dxman.deployment.data.DXManDataAlgorithm;
import com.dxman.execution.DXManWorkflowTree;
import com.dxman.design.connectors.composition.DXManSequencerTemplate;
import com.dxman.design.data.DXManOperation;
import com.dxman.design.data.DXManParameter;
import com.dxman.design.data.DXManParameterType;
import com.dxman.design.distribution.DXManBindingContent;
import com.dxman.design.distribution.DXManBindingInfo;
import com.dxman.design.distribution.DXManDeploymentInfo;
import com.dxman.design.distribution.DXManEndpointType;
import com.dxman.design.services.atomic.DXManAtomicServiceTemplate;
import com.dxman.design.services.atomic.DXManComputationUnit;
import com.dxman.design.services.common.DXManServiceInfo;
import com.dxman.design.services.common.DXManServiceTemplate;
import com.dxman.design.services.composite.DXManCompositeServiceTemplate;
import com.dxman.execution.DXManWfInvocation;
import com.dxman.execution.DXManWfNode;
import com.dxman.execution.DXManWfNodeCustom;
import com.dxman.execution.DXManWfNodeMapper;
import com.dxman.execution.DXManWfParallel;
import com.dxman.execution.DXManWfParallelCustom;
import com.dxman.execution.DXManWfResult;
import com.dxman.execution.DXManWfSequencer;
import com.dxman.execution.DXManWfSequencerCustom;
import com.dxman.utils.RuntimeTypeAdapterFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.javalite.http.Http;
import org.javalite.http.Post;

/**
 * @author Damian Arellanes
 */
public class DesignerTest {
  
  public static DXManDeploymentManager deploymentManager = new DXManDeploymentManager();
  
  private static DXManAtomicServiceTemplate designLoyaltyPointsBank() throws URISyntaxException {
    
    DXManBindingInfo createRecordBinding = new DXManBindingInfo(
      new URI("http://localhost:8080/loyaltypointsbank-microservice/api/createRecord"), 
      DXManEndpointType.HTTP_POST, 
      DXManBindingContent.APPLICATION_JSON, 
      DXManBindingContent.PLAIN, 
      "{\"name\":\"##name##\", \"address\":\"##addr##\", \"email\":\"##email##\"}", 
      "##id##"
    );
    
    DXManOperation createRecord = new DXManOperation("createRecord", createRecordBinding);
    DXManParameter name = new DXManParameter("name", DXManParameterType.INPUT, "string"); createRecord.addParameter(name);
    DXManParameter addr = new DXManParameter("addr", DXManParameterType.INPUT, "string"); createRecord.addParameter(addr);
    DXManParameter email = new DXManParameter("email", DXManParameterType.INPUT, "string"); createRecord.addParameter(email);
    DXManParameter id = new DXManParameter("id", DXManParameterType.OUTPUT, "string"); createRecord.addParameter(id);    
    
    DXManComputationUnit cu = new DXManComputationUnit();
    DXManServiceInfo templateInfo = new DXManServiceInfo("LoyaltyPointsBank", "MusicCorp", 0);
    DXManDeploymentInfo deploymentInfo = new DXManDeploymentInfo("192.168.0.5", 5683);
    DXManAtomicServiceTemplate loyaltyPointsBank = new DXManAtomicServiceTemplate(templateInfo, "IC1", cu, deploymentInfo);
    loyaltyPointsBank.addOperation(createRecord);
    
    return loyaltyPointsBank;
  }
  
  private static DXManAtomicServiceTemplate designCourier(int num, String op) throws URISyntaxException {
    
    DXManBindingInfo sendWelcBinding = new DXManBindingInfo(
      new URI("http://localhost:8080/courier" + num +"-microservice/api/" + op), 
      DXManEndpointType.HTTP_POST, 
      DXManBindingContent.QUERY_STRING, 
      DXManBindingContent.PLAIN, 
      "?addr=##addr##&name=##name##", 
      "##res##"
    );
    
    DXManOperation sendWelc = new DXManOperation(op, sendWelcBinding);
    DXManParameter addr = new DXManParameter("addr", DXManParameterType.INPUT, "string"); sendWelc.addParameter(addr);
    DXManParameter name = new DXManParameter("name", DXManParameterType.INPUT, "string"); sendWelc.addParameter(name);
    DXManParameter res = new DXManParameter("res", DXManParameterType.OUTPUT, "string"); sendWelc.addParameter(res);
    
    DXManComputationUnit cu = new DXManComputationUnit();
    DXManServiceInfo templateInfo = new DXManServiceInfo("Courier"+num, "MusicCorp", 0);
    DXManDeploymentInfo deploymentInfo = new DXManDeploymentInfo("192.168.0.5", 5683);
    DXManAtomicServiceTemplate courier = new DXManAtomicServiceTemplate(templateInfo, "IC"+(num+1), cu, deploymentInfo);
    courier.addOperation(sendWelc);
    
    return courier;
  }
  
  private static DXManAtomicServiceTemplate designEmail() throws URISyntaxException {
    
    DXManBindingInfo sendWelcEmailBinding = new DXManBindingInfo(
      new URI("http://localhost:8080/email-microservice/api/sendWelcEmail"), 
      DXManEndpointType.HTTP_POST, 
      DXManBindingContent.PLAIN, 
      DXManBindingContent.NO_CONTENT, 
      "##email##", 
      ""
    );
    
    DXManOperation sendWelcEmail = new DXManOperation("sendWelcEmail", sendWelcEmailBinding);
    DXManParameter email = new DXManParameter("email", DXManParameterType.INPUT, "string"); sendWelcEmail.addParameter(email);
    //DXManParameter res = new DXManParameter("res", DXManParameterType.OUTPUT, "string"); sendWelcEmail.addParameter(res);
    
    DXManComputationUnit cu = new DXManComputationUnit();
    DXManServiceInfo templateInfo = new DXManServiceInfo("Email Service", "MusicCorp", 0);
    DXManDeploymentInfo deploymentInfo = new DXManDeploymentInfo("192.168.0.5", 5683);
    DXManAtomicServiceTemplate emailService = new DXManAtomicServiceTemplate(templateInfo, "IC4", cu, deploymentInfo);
    emailService.addOperation(sendWelcEmail);
    
    return emailService;
  }
  
  private static DXManCompositeServiceTemplate designPost() throws URISyntaxException {
    
    DXManSequencerTemplate sequencer = new DXManSequencerTemplate("SEQ1");
    DXManAtomicServiceTemplate courier1 = designCourier(1, "sendWelcStd");
    DXManAtomicServiceTemplate courier2 = designCourier(2, "sendWelcFast");
    
    sequencer.composeServices(courier1, courier2);
    
    DXManServiceInfo templateInfo = new DXManServiceInfo("PostService", "Example", 0);
    DXManDeploymentInfo deploymentInfo = new DXManDeploymentInfo("192.168.0.5", 5683);
    DXManCompositeServiceTemplate composite = new DXManCompositeServiceTemplate(templateInfo, sequencer, deploymentInfo);
    
    return composite;
  }
  
  private static DXManCompositeServiceTemplate designSender() throws URISyntaxException {
    
    DXManSequencerTemplate sequencer = new DXManSequencerTemplate("SEQ2");
    DXManCompositeServiceTemplate post = designPost();
    DXManAtomicServiceTemplate email = designEmail();
    
    sequencer.composeServices(post, email);
    
    DXManServiceInfo templateInfo = new DXManServiceInfo("SenderService", "Example", 0);
    DXManDeploymentInfo deploymentInfo = new DXManDeploymentInfo("192.168.0.5", 5683);
    DXManCompositeServiceTemplate composite = new DXManCompositeServiceTemplate(templateInfo, sequencer, deploymentInfo);
    
    return composite;
  }
  
  private static DXManCompositeServiceTemplate designCustomer() throws URISyntaxException {
    
    DXManSequencerTemplate sequencer = new DXManSequencerTemplate("SEQ3");
    DXManCompositeServiceTemplate sender = designSender();
    DXManAtomicServiceTemplate lpb = designLoyaltyPointsBank();
    
    sequencer.composeServices(sender, lpb);
    
    DXManServiceInfo templateInfo = new DXManServiceInfo("CustomerService", "Example", 0);
    DXManDeploymentInfo deploymentInfo = new DXManDeploymentInfo("192.168.0.5", 5683);
    DXManCompositeServiceTemplate composite = new DXManCompositeServiceTemplate(templateInfo, sequencer, deploymentInfo);
    
    return composite;
  }
  
  public static void simulate(DXManWfNode wfNode) {
   
    System.out.println("Executing: " + wfNode.getUri());
    System.out.println("Workflow ID: " + wfNode.getWorkflowId());
    System.out.println("Node: " + wfNode.getId());
    
    if(wfNode.getClass().equals(DXManWfInvocation.class)) return;
    
    for(DXManWfNode subNode: ((DXManWfSequencer)wfNode).getSequence()) {
      
      simulate(subNode);
    }    
  }
  
  public static void main(String[] args) throws URISyntaxException {
        
    DXManAtomicServiceTemplate loyaltyPointsBank = designLoyaltyPointsBank();
    DXManCompositeServiceTemplate post = designPost();    
    DXManCompositeServiceTemplate sender = designSender();
    DXManCompositeServiceTemplate customer = designCustomer();
    
//    Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
//    
//    Post response = Http.post("http://localhost:8080/dxman-platform/api/deploy-atomic", GSON.toJson(loyaltyPointsBank));
//    response.header("Content-type", "application/json");
//    
//    System.out.println(response.text());    
            
        
    DXManWorkflowTreeDeployer wfTreeManager = new DXManWorkflowTreeDeployer("http://localhost:3000");
    String workflowTreeFile = "/tmp/wfTree1";
    
    // GENERATE WORKFLOW FILES    
    //wfTreeManager.buildWorkflowTree(workflowTreeFile, customer);
    
    // READS WORKFLOW FROM FILE
    DXManWorkflowTree wfTree = wfTreeManager.readWorkflowTreeDescription(workflowTreeFile);    
    WfTreeTest wtEditor = new WfTreeTest(wfTree, "e00614ec-bc02-4d27-a130-7d275450c29a");
    //WfTreeTest wtEditor = new WfTreeTest(wfTree, "ANOTHERWF");
    //WfTreeTest wtEditor = new WfTreeTest(wfTree, "INEXISTENT");
    
    // DEPLOY WORKFLOW FROM FILE
    //deploymentManager.deployCompositeService(wfTree.getCompositeService());
    wfTreeManager.deployWorkflow(wtEditor, false); // true when data channels are modified, false for using same data channels
    
    /*// EXECUTES WORKFLOW FROM FILE
    DXManWfResult wfResult = wfTreeManager.executeWorkflow(wtEditor, wfTree.getWt().get("SEQ3"), false);
    wfResult.forEach((outputId, outputVal) -> {    
      System.out.println(outputId + " --> " + outputVal);
    });*/
    
    simulate(wfTree.getWt().get(wfTree.getCompositeService().getId()));
    
    /*System.out.println("-----INPUTS-------");
    System.out.println(alg.getReaders().get("IC1.createRecord.name"));//SEQ3.createRecord.name
    System.out.println(alg.getReaders().get("IC1.createRecord.addr"));//SEQ3.createRecord.addr
    System.out.println(alg.getReaders().get("IC1.createRecord.email"));//SEQ3.createRecord.email
    System.out.println(alg.getReaders().get("IC2.sendWelcStd.addr"));//SEQ3.sendWelcStd.addr
    System.out.println(alg.getReaders().get("IC2.sendWelcStd.name"));//SEQ3.sendWelcStd.name
    System.out.println(alg.getReaders().get("IC3.sendWelcFast.addr"));//SEQ3.sendWelcFast.addr
    System.out.println(alg.getReaders().get("IC3.sendWelcFast.name"));//SEQ3.sendWelcFast.name
    System.out.println(alg.getReaders().get("IC4.sendWelcEmail.email"));//SEQ3.sendWelcEmail.email
    
    System.out.println(alg.getReaders().get("SEQ1.addr"));//SEQ3.sendWelcStd.addr
    System.out.println("-----OUTPUTS-------");
    System.out.println(alg.getReaders().get("SEQ3.sendWelcStd.res"));//IC2.sendWelcStd.res
    System.out.println(alg.getReaders().get("SEQ3.sendWelcFast.res"));//IC3.sendWelcFast.res
    System.out.println(alg.getReaders().get("SEQ3.createRecord.id"));//IC1.createRecord.id
    //System.out.println(alg.getReaders().get("SEQ3.sendWelcEmail.res"));//IC4.sendWelcEmail.email*/
  }
}
