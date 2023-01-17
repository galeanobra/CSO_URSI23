package org.nextsus.cso.auto.component;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.solution.pointsolution.PointSolution;

class TerminationByAspirationPointTest {
  private int maximumNumberOfEvaluations ;
  private List<List<Double>> referencePoints = List.of(List.of(1.0, 2.0)) ;
  private Termination termination ;
  @BeforeEach
  void setUp() {
    maximumNumberOfEvaluations = 10000;
    termination = new TerminationByAspirationPoint(referencePoints, maximumNumberOfEvaluations);
  }

  @Test
  void theIsMetMethodReturnsTrueIfTheReferencePointDominatesAPointOfASolutionList() {
    PointSolution solution1 = new PointSolution(new double[]{0.1, 3.0}) ;
    PointSolution solution2 = new PointSolution(new double[]{2.0, 1.0}) ;
    PointSolution solution3 = new PointSolution(new double[]{0.9, 1.9}) ;

    Map<String, Object> algorithmStatusData = new HashMap<>();
    algorithmStatusData.put("EVALUATIONS", 1) ;
    List<PointSolution> population = List.of(solution1, solution2, solution3) ;
    algorithmStatusData.put("POPULATION", population) ;

    assertTrue(termination.isMet(algorithmStatusData)) ;
  }

}