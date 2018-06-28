package com.dxman.design.connectors.composition;

import com.dxman.design.connectors.common.*;
import com.dxman.design.services.common.DXManServiceTemplate;
import java.util.*;

/**
 * @author Damian Arellanes
 */
public class DXManCompositionConnectorTemplate extends DXManConnectorTemplate {
    
  private final List<DXManServiceTemplate> subServices;
  
  public DXManCompositionConnectorTemplate(DXManConnectorType type) {

    super(type);
    this.subServices = new ArrayList<>();
  }
  
  public void composeServices(DXManServiceTemplate... services) {
    subServices.addAll(Arrays.asList(services));
  }
  
  public List<DXManServiceTemplate> getSubServices() { return subServices; }
}
