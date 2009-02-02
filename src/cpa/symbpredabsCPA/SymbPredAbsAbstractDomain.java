/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabsCPA;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.symbpredabs.AbstractFormulaManager;
import exceptions.CPAException;


/**
 * Abstract domain for Symbolic lazy abstraction with summaries.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsAbstractDomain implements AbstractDomain {

  private SymbPredAbsCPA cpa;

  public SymbPredAbsAbstractDomain(SymbPredAbsCPA cpa) {
    this.cpa = cpa;
  }

  private final static class SymbPredAbsBottomElement extends SymbPredAbsAbstractElement {
    public SymbPredAbsBottomElement() {
      super(null, true, null, null, null, null, null, null, null, null);
      super.isBottomElement = true;
    }

    @Override
    public String toString() {
      return "<BOTTOM>";
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      
      // TODO: Is this enough?
      return (o instanceof SymbPredAbsBottomElement);
    }
    
    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }
  }
  private final static class SymbPredAbsTopElement extends SymbPredAbsAbstractElement {
    public SymbPredAbsTopElement() {
      super(null, true, null, null, null, null, null, null, null, null);
    }
    
    @Override
    public String toString() {
      return "<TOP>";
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      
      // TODO: Is this enough
      return (o instanceof SymbPredAbsTopElement);
    }
    
    @Override
    public int hashCode() {
      return Integer.MAX_VALUE;
    }
  }

  private final static class SymbPredAbsJoinOperator implements JoinOperator {
    public AbstractElement join(AbstractElement element1,
        AbstractElement element2) throws CPAException {
      throw new CPAException("Can't join summaries!");
    }
  }

  private final class SymbPredAbsPartialOrder implements PartialOrder {
    public boolean satisfiesPartialOrder(AbstractElement element1,
        AbstractElement element2) throws CPAException {
      SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element1;
      SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)element2;

      if (e1 == bottom) {
        return true;
      } else if (e2 == top) {
        return true;
      } else if (e2 == bottom) {
        return false;
      } else if (e1 == top) {
        return false;
      }

      assert(e1.getAbstraction() != null);
      assert(e2.getAbstraction() != null);

      // TODO check later
      //if (e1.getLocation().equals(e2.getLocation())) {
        AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
        return amgr.entails(e1.getAbstraction(), e2.getAbstraction());
      //}
      // return false;
    }
  }

  private final static SymbPredAbsBottomElement bottom = new SymbPredAbsBottomElement();
  private final static SymbPredAbsTopElement top = new SymbPredAbsTopElement();
  private final static JoinOperator join = new SymbPredAbsJoinOperator();
  private final PartialOrder partial = new SymbPredAbsPartialOrder();

  public AbstractElement getBottomElement() {
    return bottom;
  }

    public boolean isBottomElement(AbstractElement element) {
      SymbPredAbsAbstractElement symbPredAbsElem = (SymbPredAbsAbstractElement) element;

//    if(predAbsElem == (domain.getBottomElement())){
//      System.out.println("==========================");
//      return true;
//    }
      // TODO if the element is the bottom element
      if(symbPredAbsElem.isBottomElement){
        return true;
      }

    return false;
    }

  public JoinOperator getJoinOperator() {
    return join;
  }

  public PartialOrder getPartialOrder() {
    return partial;
  }

  public AbstractElement getTopElement() {
    return top;
  }

  public SymbPredAbsCPA getCPA() {
    return cpa;
  }
}
