package org.nextsus.cso.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII} class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIWithConstraintsConfiguredFromAParameterString {
  public static void main(String[] args) {
    String referenceFrontFileName = "ReferenceFront.csv" ;

    String[] parameters =
        ("--problemName org.nextsus.cso.problem.StaticCSO "
                + "--referenceFrontFileName "+ referenceFrontFileName + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--algorithmResult population "
                + "--populationSize 100 "
                + "--offspringPopulationSize 100 "
                + "--createInitialSolutions random "
                + "--variation crossoverAndMutationVariation "
                + "--selection tournament "
                + "--selectionTournamentSize 2 "
                + "--rankingForSelection dominanceRanking "
                + "--densityEstimatorForSelection crowdingDistance "
                + "--crossover singlePoint "
                + "--crossoverProbability 0.9 "
                + "--mutation bitFlip "
                + "--mutationProbabilityFactor 1.0 ")
            .split("\\s+");


    var autoNSGAII = new AutoNSGAIICSOWithConstraints();
    autoNSGAII.parseAndCheckParameters(parameters);

    AutoNSGAII.print(autoNSGAII.fixedParameterList);
    AutoNSGAII.print(autoNSGAII.autoConfigurableParameterList);

    EvolutionaryAlgorithm<BinaryCSOSolution> nsgaII = autoNSGAII.create();
    EvaluationObserver evaluationObserver = new EvaluationObserver(100);

    List<List<Double>> referencePoints;
    referencePoints = new ArrayList<>();
    referencePoints.add(Arrays.asList(0.01, -1400.0));
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
           "NSGA-II", 80, 100,null);
    runTimeChartObserver.setReferencePoins(referencePoints);

    nsgaII.getObservable().register(evaluationObserver);
    nsgaII.getObservable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.getTotalComputingTime()); ;

    new SolutionListOutput(nsgaII.getResult())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
