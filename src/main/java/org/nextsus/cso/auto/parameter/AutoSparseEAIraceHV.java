package org.nextsus.cso.auto.parameter;

import org.nextsus.cso.auto.algorithm.AutoNSGAIICSO;
import org.nextsus.cso.auto.algorithm.AutoSparseEACSO;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

public class AutoSparseEAIraceHV {
    public static void main(String[] args) throws IOException {

        Problem<BinaryCSOSolution> problem = new StaticCSO("main.properties", "LL", 0);
        int maximumNumberOfEvaluations = Integer.parseInt(args[5]);
        String algorithmResult = args[7];
        int populationSize = Integer.parseInt(args[9]);
        String selection = args[11];
        int selectionTournamentSize = Integer.parseInt(args[13]);
        double crossoverProbability = Double.parseDouble(args[15]);
        double mutationProbabilityFactor = Double.parseDouble(args[17]);

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
        sparseea.execute();

        String referenceFrontFile = "referenceFront.csv";

        double[][] referenceFront = VectorUtils.readVectors(referenceFrontFile, ",");
        double[][] front = getMatrixWithObjectiveValues(sparseea.getResult());

        double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
        double[][] normalizedFront = NormalizeUtils.normalize(front, NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront), NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

        var qualityIndicator = new PISAHypervolume(normalizedReferenceFront);
        System.out.println(qualityIndicator.compute(normalizedFront) * -1.0);
    }
}
