// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.InfeasibleDummyState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicatePrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;
  private final BlockOperator blk;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView fmgr;

  private final PredicateCPAInvariantsManager invariants;
  private final PredicateProvider predicateProvider;
  private final PredicateStatistics statistics;
  private final TimerWrapper totalPrecTime;
  private final TimerWrapper computingAbstractionTime;

  public PredicatePrecisionAdjustment(
      LogManager pLogger,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      PredicateAbstractionManager pPredAbsManager,
      PredicateCPAInvariantsManager pInvariantSupplier,
      PredicateProvider pPredicateProvider,
      PredicateStatistics pPredicateStatistics) {

    logger = pLogger;
    fmgr = pFmgr;
    pathFormulaManager = pPfmgr;
    blk = pBlk;
    formulaManager = pPredAbsManager;

    invariants = pInvariantSupplier;
    predicateProvider = pPredicateProvider;
    statistics = pPredicateStatistics;
    totalPrecTime = statistics.totalPrecTime.getNewTimer();
    computingAbstractionTime = statistics.computingAbstractionTime.getNewTimer();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    totalPrecTime.start();
    try {
      PredicateAbstractState element = (PredicateAbstractState)pElement;
      CFANode location = AbstractStates.extractLocation(fullState);

      if (shouldComputeAbstraction(fullState, location, element)) {
        PredicatePrecision precision = (PredicatePrecision)pPrecision;

        return computeAbstraction(element, precision, location, fullState);
      } else {
        return Optional.of(PrecisionAdjustmentResult.create(
            element, pPrecision, PrecisionAdjustmentResult.Action.CONTINUE));
      }

    } catch (SolverException e) {
      throw new CPAException("Solver Failure: " + e.getMessage(), e);
    } finally {
      totalPrecTime.stop();
    }
  }

  private boolean shouldComputeAbstraction(
      AbstractState fullState, CFANode location, PredicateAbstractState predicateState) {
    if (predicateState.isAbstractionState()) {
      return false;
    }
    if (predicateState instanceof InfeasibleDummyState) {
      return false;
    }
    if (blk.isBlockEnd(location, predicateState.getPathFormula().getLength())) {
      return true;
    }
    if (AbstractStates.isTargetState(fullState)) {
      statistics.numTargetAbstractions.inc();
      return true;
    }
    return false;
  }

  /**
   * Compute an abstraction.
   */
  private Optional<PrecisionAdjustmentResult> computeAbstraction(
      PredicateAbstractState element,
      PredicatePrecision precision,
      CFANode loc,
      AbstractState fullState)
      throws SolverException, CPAException, InterruptedException {

    AbstractionFormula abstractionFormula = element.getAbstractionFormula();
    PersistentMap<CFANode, Integer> abstractionLocations = element.getAbstractionLocationsOnPath();
    PathFormula pathFormula = element.getPathFormula();
    Integer newLocInstance = abstractionLocations.getOrDefault(loc, 0) + 1;
    Optional<CallstackStateEqualsWrapper> callstackWrapper =
        AbstractStates.extractOptionalCallstackWraper(fullState);

    statistics.numAbstractions.inc();
    logger.log(Level.FINEST, "Computing abstraction at instance", newLocInstance, "of node", loc, "in path.");

    statistics.blockSize.setNextValue(pathFormula.getLength());

    // update/get invariants and add them, the need to be instantiated
    // (we do only update global invariants (computed by a parallelalgorithm) here
    // as everything else can only be computed during refinement)
    invariants.updateGlobalInvariants();
    BooleanFormula invariant;
    if (invariants.appendToPathFormula()) {
      invariant =
          fmgr.instantiate(
              invariants.getInvariantFor(loc, callstackWrapper, fmgr, pathFormulaManager, pathFormula),
              pathFormula.getSsa());
    } else {
      invariant = fmgr.getBooleanFormulaManager().makeTrue();
    }

    // we don't want to add trivially true invariants
    if (!fmgr.getBooleanFormulaManager().isTrue(invariant)) {
      pathFormula = pathFormulaManager.makeAnd(pathFormula, invariant);
    }

    // get additional predicates
    Set<AbstractionPredicate> additionalPredicates = predicateProvider.getPredicates(fullState);

    AbstractionFormula newAbstractionFormula = null;

    // compute new abstraction
    computingAbstractionTime.start();
    try {
      Set<AbstractionPredicate> preds = precision.getPredicates(loc, newLocInstance);
      preds = Sets.union(preds, additionalPredicates);

      // compute a new abstraction with a precision based on `preds`
      newAbstractionFormula = formulaManager.buildAbstraction(
          loc, callstackWrapper, abstractionFormula, pathFormula, preds);
    } finally {
      computingAbstractionTime.stop();
    }

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstractionFormula.isFalse()) {
      statistics.numAbstractionsFalse.inc();
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
      return Optional.empty();
    }

    // create new empty path formula
    PathFormula newPathFormula = pathFormulaManager.makeEmptyPathFormula(pathFormula);

    // initialize path formula with current invariants
    if (!fmgr.getBooleanFormulaManager().isTrue(invariant)) {
      newPathFormula = pathFormulaManager.makeAnd(newPathFormula, invariant);
    }

    // update abstraction locations map
    abstractionLocations = abstractionLocations.putAndCopy(loc, newLocInstance);

    PredicateAbstractState state =
        PredicateAbstractState.mkAbstractionState(newPathFormula,
            newAbstractionFormula,
            abstractionLocations,
            element.getPreviousAbstractionState());
    return Optional.of(PrecisionAdjustmentResult.create(
        state, precision, PrecisionAdjustmentResult.Action.CONTINUE));
  }
}
