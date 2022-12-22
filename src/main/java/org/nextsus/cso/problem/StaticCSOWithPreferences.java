package org.nextsus.cso.problem;

import org.nextsus.cso.solution.BinaryCSOSolution;

public class StaticCSOWithPreferences extends StaticCSO {

  /**
   * Creates an instance of the Static CSO problem
   *
   * @param mainConfig
   * @param scenario
   * @param run
   */
  public StaticCSOWithPreferences(String mainConfig, String scenario, int run) {
    super(mainConfig, scenario, run);
  }

  @Override
  public int numberOfConstraints() {
    return 2;
  }

  public BinaryCSOSolution evaluate(BinaryCSOSolution solution) {
    super.evaluate(solution);
    this.evaluatePreferences(solution);

    return solution;
  }

  private BinaryCSOSolution evaluatePreferences(BinaryCSOSolution solution) {
    solution.constraints()[0] =  solution.objectives()[0] + 0.016;
    solution.constraints()[1] =  solution.objectives()[1] - 1400;


    return solution;
  }
}
