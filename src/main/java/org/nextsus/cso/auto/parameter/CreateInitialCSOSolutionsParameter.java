package org.nextsus.cso.auto.parameter;

import java.util.List;
import org.nextsus.cso.problem.BinaryCSOProblem;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class CreateInitialCSOSolutionsParameter extends CategoricalParameter {

  public CreateInitialCSOSolutionsParameter(String[] args, List<String> validValues) {
    this("createInitialSolutions", args, validValues);
  }

  public CreateInitialCSOSolutionsParameter(String parameterName, String[] args,
      List<String> validValues) {
    super(parameterName, args, validValues);
  }

  public SolutionsCreation<DoubleSolution> getParameter(DoubleProblem problem, int populationSize) {
    switch (getValue()) {
      case "random":
        return new RandomSolutionsCreation<>(problem, populationSize);
      case "scatterSearch":
        return new ScatterSearchSolutionsCreation(problem, populationSize, 4);
      case "latinHypercubeSampling":
        return new LatinHypercubeSamplingSolutionsCreation(problem, populationSize);
      default:
        throw new JMetalException(
            getValue() + " is not a valid initialization strategy");
    }
  }

  public SolutionsCreation<BinaryCSOSolution> getParameter(BinaryCSOProblem problem, int populationSize) {
    switch (getValue()) {
      case "random":
        return new RandomSolutionsCreation<>(problem, populationSize);
      default:
        throw new JMetalException(
            getValue() + " is not a valid initialization strategy");
    }
  }
}