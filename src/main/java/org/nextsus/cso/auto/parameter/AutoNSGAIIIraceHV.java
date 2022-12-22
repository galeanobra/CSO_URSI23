package org.nextsus.cso.auto.parameter;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import org.nextsus.cso.auto.AutoNSGAIICSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;

public class AutoNSGAIIIraceHV {
  public static void main(String[] args) throws IOException {
    AutoNSGAIICSO nsgaii = new AutoNSGAIICSO();
    nsgaii.parseAndCheckParameters(args);

    EvolutionaryAlgorithm<BinaryCSOSolution> nsgaII = nsgaii.create();
    nsgaII.run();

    String referenceFrontFile = "referenceFront.csv" ;

    double[][] referenceFront = VectorUtils.readVectors(referenceFrontFile, ",");
    double[][] front = getMatrixWithObjectiveValues(nsgaII.getResult()) ;

    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront =
            NormalizeUtils.normalize(
                    front,
                    NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
                    NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    var qualityIndicator = new PISAHypervolume(normalizedReferenceFront) ;
    System.out.println(qualityIndicator.compute(normalizedFront) * -1.0) ;
  }
}
