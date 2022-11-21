package org.nextsus.cso.problem;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.AbstractGenericProblem;

@SuppressWarnings("serial")
public abstract class AbstractBinaryCSOProblem extends AbstractGenericProblem<BinaryCSOSolution>
        implements BinaryCSOProblem {

  @Override
  public int getBitsFromVariable(int index) {
    return getListOfBitsPerVariable().get(index);
  }

  @Override
  public int getTotalNumberOfBits() {
    int count = 0;
    for (int i = 0; i < this.getNumberOfVariables(); i++) {
      count += this.getListOfBitsPerVariable().get(i);
    }

    return count;
  }

  @Override
  public BinaryCSOSolution createSolution() {
    return new BinaryCSOSolution(getListOfBitsPerVariable(), getNumberOfObjectives(), getNumberOfConstraints());
  }
}
