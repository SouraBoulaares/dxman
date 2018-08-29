package com.dxman.execution.selector;

import com.dxman.execution.common.DXManWfSpec;
import com.dxman.execution.common.DXManWfNodeMapper;
import com.dxman.execution.common.DXManWfNode;

/**
 * @author Damian Arellanes
 */
public class DXManWfSelector extends DXManWfNode {
    
  public DXManWfSelector() {}

  public DXManWfSelector(String id, String uri) {
    super(id, uri);
  }
  
  @Override
  public boolean isValid() {
    
    for(DXManWfNodeMapper subNodeMapper: getSubnodeMappers()) {      
      
      if(subNodeMapper.getNode() == null || !subNodeMapper.getNode().isValid()
        || subNodeMapper.getCustom() == null 
        || !subNodeMapper.getCustom().getClass().equals(DXManWfSelectorCustom.class)
        || ((DXManWfSelectorCustom)subNodeMapper.getCustom()).getCondition() == null
        || ((DXManWfSelectorCustom)subNodeMapper.getCustom()).getCondition().getOperator() == null
      ) return false;
    }
    
    return !getSubnodeMappers().isEmpty();
  }
  
  @Override
  public DXManWfSpec build() {
    return new DXManWfSpec(getId()+"-wf-spec", this);
  }
}
