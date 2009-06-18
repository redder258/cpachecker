package compositeCPA;

import java.util.Collection;
import java.util.List;

import cpa.art.ARTElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.RefinementManager;

public class CompositeRefinementManager implements RefinementManager{

  private final List<RefinementManager> refinementManagers;
  
  public CompositeRefinementManager(List<RefinementManager> pRefinementManagers) {
    refinementManagers = pRefinementManagers;
  }

  @Override
  public boolean performRefinement(
      Collection<AbstractElementWithLocation> pReached) {
    // TODO Auto-generated method stub
    return false;
  }
  
}
