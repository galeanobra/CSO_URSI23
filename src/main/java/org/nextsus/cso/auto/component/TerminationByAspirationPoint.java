package org.nextsus.cso.auto.component;

import static org.uma.jmetal.util.VectorUtils.dominanceTest;

import java.util.List;
import java.util.Map;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Class that allows to check the termination condition when current front is above a given
 * percentage of the value of a quality indicator applied to a reference front. An evaluations limit
 * is used to avoid an infinite loop if the value is never achieved.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class TerminationByAspirationPoint implements Termination {
  private final List<List<Double>> aspirationPoints;
  private final int evaluationsLimit ;
  private int evaluations;
  private boolean evaluationsLimitReached;

  public TerminationByAspirationPoint(
      List<List<Double>> aspirationPoints,
      int evaluationsLimit) {
    Check.notNull(aspirationPoints);
    Check.valueIsNotNegative(evaluationsLimit);


    this.aspirationPoints = aspirationPoints ;
    this.evaluationsLimit = evaluationsLimit;
    evaluationsLimitReached = false;
  }

  @Override
  public boolean isMet(Map<String, Object> algorithmStatusData) {
    Check.notNull(algorithmStatusData.get("POPULATION"));
    Check.notNull(algorithmStatusData.get("EVALUATIONS"));

    List<Solution<?>> population = (List<Solution<?>>) algorithmStatusData.get("POPULATION");
    evaluations = (int) algorithmStatusData.get("EVALUATIONS");
    Check.notNull(population);
    Check.collectionIsNotEmpty(population);

    double[] aspirationPoint = aspirationPoints.get(0).stream().mapToDouble(Double::doubleValue).toArray();

    boolean stoppingCondition = false;
    boolean unsuccessfulStopCondition = evaluationsLimit <= evaluations;
    if (unsuccessfulStopCondition) {
      evaluationsLimitReached = true;
      stoppingCondition = true ;
    } else {

      stoppingCondition = population.stream().anyMatch(s -> dominanceTest(s.objectives(), aspirationPoint) == -1) ;

      /*
      for (Solution<?> solution : population) {
        double[] point = solution.objectives() ;
        if (dominanceTest(point, aspirationPoint) == -1) {
          stoppingCondition = true;

          break;
        }
      }

       */
      /*
      if (stoppingCondition) {
        new SolutionListOutput(population)
            .setVarFileOutputContext(new DefaultFileOutputContext("VAR." + evaluations + ".csv", ","))
            .setFunFileOutputContext(new DefaultFileOutputContext("FUN." + evaluations + ".csv", ","))
            .print();
      }
       */
    }
    return stoppingCondition;
  }

  public boolean evaluationsLimitReached() {
    return evaluationsLimitReached;
  }

  public int getEvaluationsLimit() {
    return evaluationsLimit ;
  }
}
