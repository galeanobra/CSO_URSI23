package org.nextsus.cso.auto.component;

import java.util.List;
import java.util.Map;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Class that allows to check the termination condition when current front is above a given
 * percentage of the value of a quality indicator applied to a reference front. An evaluations limit
 * is used to avoid an infinite loop if the value is never achieved.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class TerminationWhenFindingAFeasibleSolution implements Termination {
  private final int evaluationsLimit ;
  private int evaluations;
  private boolean evaluationsLimitReached;

  public TerminationWhenFindingAFeasibleSolution(
      int evaluationsLimit) {
    Check.valueIsNotNegative(evaluationsLimit);

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

    boolean stoppingCondition = false;
    boolean unsuccessfulStopCondition = evaluationsLimit <= evaluations;
    if (unsuccessfulStopCondition) {
      evaluationsLimitReached = true;
      stoppingCondition = true ;
    } else {

      stoppingCondition = population.stream().anyMatch(ConstraintHandling::isFeasible) ;

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
