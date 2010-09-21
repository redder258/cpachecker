/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.art;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public abstract class AbstractARTBasedRefiner implements Refiner {

  private final ARTCPA mArtCpa;
  private final LogManager logger;

  private final Set<Path> seenCounterexamples = Sets.newHashSet();

  protected AbstractARTBasedRefiner(ConfigurableProgramAnalysis pCpa) throws CPAException {
    if (!(pCpa instanceof ARTCPA)) {
      throw new CPAException("ARTCPA needed for refinement");
    }
    mArtCpa = (ARTCPA)pCpa;
    this.logger = mArtCpa.getLogger();
  }

  protected ARTCPA getArtCpa() {
    return mArtCpa;
  }

  private static final Function<Pair<ARTElement, CFAEdge>, String> pathToFunctionCalls
        = new Function<Pair<ARTElement, CFAEdge>, String>() {
    @Override
    public String apply(Pair<ARTElement,CFAEdge> arg) {

      if (arg.getSecond() instanceof FunctionCallEdge) {
        FunctionCallEdge funcEdge = (FunctionCallEdge)arg.getSecond();
        return "line " + funcEdge.getLineNumber() + ":\t" + funcEdge.getRawStatement();
      } else {
        return null;
      }
    }
  };

  @Override
  public final boolean performRefinement(ReachedSet pReached) throws CPAException {
    logger.log(Level.FINEST, "Starting ART based refinement");

    assert checkART(pReached);

    AbstractElement lastElement = pReached.getLastElement();
    assert lastElement instanceof ARTElement;
    Path path = buildPath((ARTElement)lastElement);
    assert pReached.getFirstElement() == path.getFirst().getFirst();

    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, "Error path:\n", path);
      logger.log(Level.ALL, "Function calls on Error path:\n",
          Joiner.on("\n ").skipNulls().join(Collections2.transform(path, pathToFunctionCalls)));
    }

    assert seenCounterexamples.add(path);

    boolean result = performRefinement(new ARTReachedSet(pReached, mArtCpa), path);

    assert checkART(pReached);

    if (!result) {
      Path targetPath = getTargetPath();
      
      if (targetPath == null) {
        targetPath = path;
      
      } else {
        // new targetPath must contain root and error node
        assert targetPath.getFirst().getFirst() == path.getFirst().getFirst();
        assert targetPath.getLast().getFirst()  == path.getLast().getFirst();
      }
      
      mArtCpa.setTargetPath(targetPath);
    }
    
    logger.log(Level.FINEST, "ART based refinement finished, result is", result);

    return result;
  }


  /**
   * Perform refinement.
   * @param pReached
   * @param pPath
   * @return whether the refinement was successful
   */
  protected abstract boolean performRefinement(ARTReachedSet pReached, Path pPath)
            throws CPAException;

  /**
   * This method is intended to be overwritten if the implementation is able to
   * provide a better target path than ARTCPA. This is probably the case when the
   * ART is a DAG and not a tree.
   * 
   * This method is called after {@link #performRefinement(ARTReachedSet, Path)}
   * and only if the former method returned false. This method should then return
   * the error path belonging to the latest call to performRefinement, or null
   * if it doesn't know better.
   * 
   * @return Null or a path from the root node to the error node.
   */
  protected Path getTargetPath() {
    return null;
  }
  
  /**
   * Create a path in the ART from root to the given element.
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  private static Path buildPath(ARTElement pLastElement) {
    Path path = new Path();
    Set<ARTElement> seenElements = new HashSet<ARTElement>();

    // each element of the path consists of the abstract element and the outgoing
    // edge to its successor

    ARTElement currentARTElement = pLastElement;
    assert pLastElement.isTarget();
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFAEdge lastEdge = currentARTElement.retrieveLocationElement().getLocationNode().getLeavingEdge(0);
    path.addFirst(new Pair<ARTElement, CFAEdge>(currentARTElement, lastEdge));
    seenElements.add(currentARTElement);

    while (!currentARTElement.getParents().isEmpty()) {
      Iterator<ARTElement> parents = currentARTElement.getParents().iterator();

      ARTElement parentElement = parents.next();
      while (!seenElements.add(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      CFAEdge edge = parentElement.getEdgeToChild(currentARTElement);
      path.addFirst(new Pair<ARTElement, CFAEdge>(parentElement, edge));

      currentARTElement = parentElement;
    }
    return path;
  }

  private static boolean checkART(ReachedSet pReached) {
    Set<? extends AbstractElement> reached = pReached.getReached();

    Deque<AbstractElement> workList = new ArrayDeque<AbstractElement>();
    Set<ARTElement> art = new HashSet<ARTElement>();

    workList.add(pReached.getFirstElement());
    while (!workList.isEmpty()) {
      ARTElement currentElement = (ARTElement)workList.removeFirst();
      for (ARTElement parent : currentElement.getParents()) {
        assert parent.getChildren().contains(currentElement);
      }
      for (ARTElement child : currentElement.getChildren()) {
        assert child.getParents().contains(currentElement);
      }

      // check if (e \in ART) => (e \in Reached ^ e.isCovered())
      assert reached.contains(currentElement) ^ currentElement.isCovered();

      if (art.add(currentElement)) {
        workList.addAll(currentElement.getChildren());
      }
    }

    // check if (e \in Reached) => (e \in ART)
    assert art.containsAll(reached) : "Element in reached but not in ART";

    return true;
  }
}
