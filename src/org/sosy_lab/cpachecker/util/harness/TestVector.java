/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class TestVector {

  private final PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<Integer>> undefinedPointerTypeFunctionIndices;

  private final PersistentSortedMap<
          ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>>
      inputFunctionValues;

  private final PersistentSortedMap<ComparableVariableDeclaration, InitializerTestValue>
      inputVariableValues;

  private final int externPointersArrayLength;

  public TestVector setExternPointersArrayLength(int newLength) {
    return new TestVector(
        inputFunctionValues,
        inputVariableValues,
        undefinedPointerTypeFunctionIndices,
        newLength,
        functionDeclarations);
  }

  public int getExternPointersArrayLength() {
    return externPointersArrayLength;
  }

  public PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<Integer>>
      getPointerIndices() {
    return undefinedPointerTypeFunctionIndices;
  }

  private ImmutableList<AFunctionDeclaration> functionDeclarations;

  private TestVector() {
    this(
        PathCopyingPersistentTreeMap
            .<ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>>of(),
        PathCopyingPersistentTreeMap.<ComparableVariableDeclaration, InitializerTestValue>of(),
        ImmutableList.of());
  }



  private TestVector(
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>>
          pInputFunctionValues,
      PersistentSortedMap<ComparableVariableDeclaration, InitializerTestValue>
      pInputVariableValues,
      ImmutableList<AFunctionDeclaration> pFunctionDeclarations) {
    inputFunctionValues = pInputFunctionValues;
    inputVariableValues = pInputVariableValues;
    undefinedPointerTypeFunctionIndices = PathCopyingPersistentTreeMap.of();
    externPointersArrayLength = 0;
    functionDeclarations = pFunctionDeclarations;
  }

  private TestVector(
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>> pInputFunctionValues,
      PersistentSortedMap<ComparableVariableDeclaration, InitializerTestValue> pInputVariableValues,
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<Integer>> pUndefinedPointerTypeFunctionIndices,
      ImmutableList<AFunctionDeclaration> pFunctionDeclarations) {
    inputFunctionValues = pInputFunctionValues;
    inputVariableValues = pInputVariableValues;
    undefinedPointerTypeFunctionIndices = pUndefinedPointerTypeFunctionIndices;
    externPointersArrayLength = 0;
    functionDeclarations = pFunctionDeclarations;
  }

  private TestVector(
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<ExpressionTestValue>> pInputFunctionValues,
      PersistentSortedMap<ComparableVariableDeclaration, InitializerTestValue> pInputVariableValues,
      PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<Integer>> pUndefinedPointerTypeFunctionIndices,
      int pArrayLength,
      ImmutableList<AFunctionDeclaration> pFunctionDeclarations) {
    inputFunctionValues = pInputFunctionValues;
    inputVariableValues = pInputVariableValues;
    undefinedPointerTypeFunctionIndices = pUndefinedPointerTypeFunctionIndices;
    externPointersArrayLength = pArrayLength;
    functionDeclarations = pFunctionDeclarations;
  }

  public TestVector
      addPointerFunctionIndex(ComparableFunctionDeclaration pFunctionDeclaration, int pIndex) {
    ImmutableList.Builder<Integer> intListBuilder = ImmutableList.builder();
    ImmutableList<Integer> oldPointerIndices =
        undefinedPointerTypeFunctionIndices.get(pFunctionDeclaration);
    if (oldPointerIndices == null) {
      oldPointerIndices = ImmutableList.of();
    }
    intListBuilder.addAll(oldPointerIndices).add(pIndex);
    ImmutableList<Integer> newIndices = intListBuilder.build();

    PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<Integer>> newFunctionIndices =
        undefinedPointerTypeFunctionIndices.putAndCopy(pFunctionDeclaration, newIndices);
    return new TestVector(
        inputFunctionValues,
        inputVariableValues,
        newFunctionIndices,
        externPointersArrayLength,
        functionDeclarations);
  }

  public TestVector addInputValue(AFunctionDeclaration pFunction, AExpression pValue) {
    return addInputValue(pFunction, ExpressionTestValue.of(pValue));
  }

  public TestVector addInputValue(AFunctionDeclaration pFunction, ExpressionTestValue pValue) {
    ComparableFunctionDeclaration function = new ComparableFunctionDeclaration(pFunction);
    ImmutableList<ExpressionTestValue> currentValues = inputFunctionValues.get(function);
    ImmutableList<ExpressionTestValue> newValues;
    if (currentValues == null) {
      newValues = ImmutableList.of(pValue);
    } else {
      ImmutableList.Builder<ExpressionTestValue> valueListBuilder = ImmutableList.builder();
      valueListBuilder.addAll(currentValues).add(pValue);
      newValues = valueListBuilder.build();
    }
    return new TestVector(
        inputFunctionValues.putAndCopy(function, newValues),
        inputVariableValues,
        undefinedPointerTypeFunctionIndices,
        externPointersArrayLength,
        functionDeclarations);
  }

  public TestVector addInputValue(AVariableDeclaration pVariable, AInitializer pValue) {
    return addInputValue(pVariable, InitializerTestValue.of(pValue));
  }

  public TestVector addInputValue(AVariableDeclaration pVariable, InitializerTestValue pValue) {
    ComparableVariableDeclaration variable = new ComparableVariableDeclaration(pVariable);
    InitializerTestValue currentValue = inputVariableValues.get(variable);
    if (currentValue != null) {
      throw new IllegalArgumentException(
          String.format("Variable %s already declared with value %s: ", pVariable, pValue));
    }
    return new TestVector(
        inputFunctionValues,
        inputVariableValues.putAndCopy(variable, pValue),
        undefinedPointerTypeFunctionIndices,
        externPointersArrayLength,
        functionDeclarations);
  }

  public Iterable<AFunctionDeclaration> getInputFunctions() {
    return FluentIterable.from(inputFunctionValues.keySet()).transform(f -> f.declaration);
  }

  public List<ExpressionTestValue> getInputValues(AFunctionDeclaration pFunction) {
    ComparableFunctionDeclaration function = new ComparableFunctionDeclaration(pFunction);
    ImmutableList<ExpressionTestValue> currentValues = inputFunctionValues.get(function);
    if (currentValues == null) {
      return Collections.emptyList();
    }
    return currentValues;
  }

  public Iterable<AVariableDeclaration> getInputVariables() {
    return FluentIterable.from(inputVariableValues.keySet()).transform(f -> f.declaration);
  }

  public InitializerTestValue getInputValue(AVariableDeclaration pDeclaration) {
    ComparableVariableDeclaration variable = new ComparableVariableDeclaration(pDeclaration);
    InitializerTestValue currentValue = inputVariableValues.get(variable);
    if (currentValue == null) {
      throw new IllegalArgumentException("Unknown variable: " + pDeclaration);
    }
    return currentValue;
  }

  public boolean contains(AFunctionDeclaration pFunctionDeclaration) {
    return inputFunctionValues.containsKey(new ComparableFunctionDeclaration(pFunctionDeclaration));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + inputFunctionValues.hashCode();
    result = prime * result + inputVariableValues.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof TestVector) {
      TestVector other = (TestVector) pObj;
      return inputFunctionValues.equals(other.inputFunctionValues)
          && inputVariableValues.equals(other.inputVariableValues);
    }
    return false;
  }

  @Override
  public String toString() {
    return inputFunctionValues.toString() + inputVariableValues.toString();
  }

  public static TestVector newTestVector() {
    return new TestVector();
  }

  public static final Ordering<FileLocation> FILE_LOCATION_ORDERING =
      Ordering.from(
          (pA, pB) -> {
            return ComparisonChain.start()
                .compare(pA.getFileName(), pB.getFileName())
                .compare(pA.getNodeOffset(), pB.getNodeOffset())
                .compare(pA.getNodeLength(), pB.getNodeLength())
                .result();
          });

  public static final Ordering<AParameterDeclaration> PARAMETER_ORDERING =
      Ordering.from(
          (pA, pB) -> {
            return ComparisonChain.start()
                .compare(pA.getQualifiedName(), pB.getQualifiedName())
                .compare(pA.getType(), pB.getType(), Ordering.usingToString())
                .compare(pA.getFileLocation(), pB.getFileLocation(), FILE_LOCATION_ORDERING)
                .result();
          });

  private static class ComparableVariableDeclaration
      implements Comparable<ComparableVariableDeclaration> {

    private final AVariableDeclaration declaration;

    public ComparableVariableDeclaration(AVariableDeclaration pDeclaration) {
      this.declaration = Objects.requireNonNull(pDeclaration);
    }

    @Override
    public int compareTo(ComparableVariableDeclaration pOther) {
      if (declaration.equals(pOther.declaration)) {
        return 0;
      }
      return ComparisonChain.start()
          .compare(declaration.getQualifiedName(), pOther.declaration.getQualifiedName())
          .compare(
              PredefinedTypes.getCanonicalType(declaration.getType()),
              PredefinedTypes.getCanonicalType(pOther.declaration.getType()),
              Ordering.usingToString())
          .compareFalseFirst(declaration.isGlobal(), pOther.declaration.isGlobal())
          .result();
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof ComparableVariableDeclaration) {
        return declaration.equals(((ComparableVariableDeclaration) pObj).declaration);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return declaration.hashCode();
    }

    @Override
    public String toString() {
      return declaration.toString();
    }
  }

  public static <T> Iterable<T> upcast(Iterable<? extends T> pIterable, Class<T> pClass) {
    return FluentIterable.from(pIterable).filter(pClass);
  }

  public ImmutableList<Integer> getIndices(ComparableFunctionDeclaration pFunctionDeclaration) {
    List<Integer> result = this.undefinedPointerTypeFunctionIndices.get(pFunctionDeclaration);
    if (result == null) {
      result = ImmutableList.of();
    }
    return ImmutableList.copyOf(result);
  }

  public boolean returnsPointers() {
    // TODO Auto-generated method stub
    return false;
  }

  public ImmutableList<ComparableFunctionDeclaration> getPointerFunctions() {
    List<ComparableFunctionDeclaration> result =
        this.undefinedPointerTypeFunctionIndices.keySet()
            .stream()
            .collect(Collectors.toList());
    return ImmutableList.copyOf(result);
  }

  public TestVector addFunctionDeclaration(AFunctionDeclaration pFunctionDeclaration) {
    ImmutableList.Builder<AFunctionDeclaration> functionDeclarationBuilder =
        ImmutableList.builder();
    if (!functionDeclarations.isEmpty()) {
      functionDeclarationBuilder.addAll(functionDeclarations).add(pFunctionDeclaration);
    } else {
      functionDeclarationBuilder.add(pFunctionDeclaration);
    }
    ImmutableList<AFunctionDeclaration> newFunctionDeclarations =
        functionDeclarationBuilder.build();
    return new TestVector(
        inputFunctionValues,
        inputVariableValues,
        undefinedPointerTypeFunctionIndices,
        externPointersArrayLength,
        newFunctionDeclarations);
  }

  public ImmutableList<AFunctionDeclaration> getFunctionDeclarations() {
    return functionDeclarations;
  }
}
