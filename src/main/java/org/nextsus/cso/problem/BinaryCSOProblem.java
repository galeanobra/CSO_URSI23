package org.nextsus.cso.problem;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;

import java.util.List;

/**
 * Interface representing binary problems
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public interface BinaryCSOProblem extends Problem<BinaryCSOSolution> {
  List<Integer> getListOfBitsPerVariable() ;
  int getBitsFromVariable(int index) ;
  int getTotalNumberOfBits() ;
}
