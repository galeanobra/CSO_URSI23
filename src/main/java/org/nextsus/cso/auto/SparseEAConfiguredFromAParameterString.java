package org.nextsus.cso.auto;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.nextsus.cso.auto.algorithm.AutoSparseEACSO;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII} class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class SparseEAConfiguredFromAParameterString {
    public static void main(String[] args) {
        String referenceFrontFileName = "ReferenceFront.csv";

        String[] parameters =
                ("--problemName org.nextsus.cso.problem.StaticCSO "
                        + "--referenceFrontFileName " + referenceFrontFileName + " "
                        + "--maximumNumberOfEvaluations 2000 "
                        + "--algorithmResult externalArchive "
                        + "--populationSize 100 "
                        + "--selection random "
                        + "--selectionTournamentSize 2 "
                        + "--crossoverProbability 0.9 "
                        + "--mutationProbabilityFactor 1.0 ")
                        .split("\\s+");

        Problem<BinaryCSOSolution> problem = new StaticCSO("main.properties", "LL", 0);
        int maximumNumberOfEvaluations = Integer.parseInt(parameters[5]);
        String algorithmResult = parameters[7];
        int populationSize = Integer.parseInt(parameters[9]);
        String selection = parameters[11];
        int selectionTournamentSize = Integer.parseInt(parameters[13]);
        double crossoverProbability = Double.parseDouble(parameters[15]);
        double mutationProbabilityFactor = Double.parseDouble(parameters[17]);

        Archive<BinaryCSOSolution> archive = null;
        if (algorithmResult.equals("externalArchive")) {
            archive = new CrowdingDistanceArchive<>(populationSize);
        }

        SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selectionOperator = null;
        if (selection.equals("tournament")) {
            Ranking<BinaryCSOSolution> ranking = new FastNonDominatedSortRanking<>(new DefaultDominanceComparator<>());
            DensityEstimator<BinaryCSOSolution> densityEstimator = new CrowdingDistanceDensityEstimator<>();
            MultiComparator<BinaryCSOSolution> rankingAndCrowdingComparator = new MultiComparator<>(Arrays.asList(Comparator.comparing(ranking::getRank), Comparator.comparing(densityEstimator::getValue).reversed()));
            selectionOperator = new NaryTournamentSelection<>(selectionTournamentSize, rankingAndCrowdingComparator);
        } else if (selection.equals("random")) {
            selectionOperator = new RandomSelection<>();
        }

        AutoSparseEACSO<BinaryCSOSolution> sparseea = new AutoSparseEACSO<>(problem, maximumNumberOfEvaluations, populationSize, crossoverProbability, mutationProbabilityFactor, selectionOperator, ((StaticCSO) problem).getTotalNumberOfActivableCells(), archive);

        RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
            new RunTimeChartObserver<>(
                "SparseEA", 80, 1,null);
        sparseea.observable.register(runTimeChartObserver);

        sparseea.execute();

        JMetalLogger.logger.info("Total computing time: " + sparseea.getTotalComputingTime());

        new SolutionListOutput(sparseea.getResult())
                .setVarFileOutputContext(new DefaultFileOutputContext("VAR.SparseEA.csv", ","))
                .setFunFileOutputContext(new DefaultFileOutputContext("FUN.SparseEA.csv", ","))
                .print();
    }
}
