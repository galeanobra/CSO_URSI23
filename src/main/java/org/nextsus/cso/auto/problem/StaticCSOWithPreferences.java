package org.nextsus.cso.auto.problem;

import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;

public class StaticCSOWithPreferences extends StaticCSO {

  private final double[] aspirationPoint ;
  /**
   * Creates an instance of the Static CSO problem
   *
   * @param mainConfig
   * @param scenario
   * @param run
   */
  public StaticCSOWithPreferences(String mainConfig, String scenario, int run, double[] aspirationPoint) {
    super(mainConfig, scenario, run);
    this.aspirationPoint = aspirationPoint ;
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
    solution.constraints()[0] =  aspirationPoint[0] -  solution.objectives()[0] ;
    solution.constraints()[1] =  aspirationPoint[1] - solution.objectives()[1] ;

    return solution;
  }
}
