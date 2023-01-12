package org.nextsus.cso.auto.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.nextsus.cso.auto.parameter.CreateInitialCSOSolutionsParameter;
import org.nextsus.cso.auto.parameter.VariationCSOParameter;
import org.nextsus.cso.problem.BinaryCSOProblem;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.IntegerParameter;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.auto.parameter.PositiveIntegerValue;
import org.uma.jmetal.auto.parameter.RealParameter;
import org.uma.jmetal.auto.parameter.StringParameter;
import org.uma.jmetal.auto.parameter.catalogue.CrossoverParameter;
import org.uma.jmetal.auto.parameter.catalogue.ExternalArchiveParameter;
import org.uma.jmetal.auto.parameter.catalogue.MutationParameter;
import org.uma.jmetal.auto.parameter.catalogue.ProbabilityParameter;
import org.uma.jmetal.auto.parameter.catalogue.SelectionParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement.RemovalPolicy;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.util.RankingAndDensityEstimatorPreference;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.GDominanceComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Class to configure NSGA-II with an argument string using class {@link EvolutionaryAlgorithm}
 *
 * @autor Antonio J. Nebro
 */
public class AutoGNSGAIICSO implements AutoConfigurableAlgorithm {
  public List<Parameter<?>> autoConfigurableParameterList = new ArrayList<>();
  public List<Parameter<?>> fixedParameterList = new ArrayList<>();
  private StringParameter problemNameParameter;
  public StringParameter referenceFrontFilename;
  private PositiveIntegerValue maximumNumberOfEvaluationsParameter;
  private CategoricalParameter algorithmResultParameter;
  private ExternalArchiveParameter<BinaryCSOSolution> externalArchiveParameter;
  private PositiveIntegerValue populationSizeParameter;
  private IntegerParameter populationSizeWithArchiveParameter;
  private IntegerParameter offspringPopulationSizeParameter;
  private CreateInitialCSOSolutionsParameter createInitialSolutionsParameter;
  private SelectionParameter<BinaryCSOSolution> selectionParameter;
  private VariationCSOParameter variationParameter;

  private MutationParameter mutationParameter ;

  @Override
  public List<Parameter<?>> getAutoConfigurableParameterList() {
    return autoConfigurableParameterList;
  }
  @Override
  public void parseAndCheckParameters(String[] args) {
    problemNameParameter = new StringParameter("problemName", args);
    referenceFrontFilename = new StringParameter("referenceFrontFileName", args);
    maximumNumberOfEvaluationsParameter =
        new PositiveIntegerValue("maximumNumberOfEvaluations", args);

    fixedParameterList.add(problemNameParameter);
    fixedParameterList.add(referenceFrontFilename);
    fixedParameterList.add(maximumNumberOfEvaluationsParameter);

    for (Parameter<?> parameter : fixedParameterList) {
      parameter.parse().check();
    }
    populationSizeParameter = new PositiveIntegerValue("populationSize", args);

    algorithmResult(args);
    createInitialSolution(args);
    selection(args);
    variation(args);

    autoConfigurableParameterList.add(populationSizeParameter);
    autoConfigurableParameterList.add(algorithmResultParameter);
    autoConfigurableParameterList.add(createInitialSolutionsParameter);
    autoConfigurableParameterList.add(variationParameter);
    autoConfigurableParameterList.add(selectionParameter);

    for (Parameter<?> parameter : autoConfigurableParameterList) {
      parameter.parse().check();
    }
  }

  private void variation(String[] args) {
    CrossoverParameter crossoverParameter = new CrossoverParameter(args,
        List.of("singlePoint", "HUX", "uniform"));
    ProbabilityParameter crossoverProbability =
        new ProbabilityParameter("crossoverProbability", args);
    crossoverParameter.addGlobalParameter(crossoverProbability);

    mutationParameter =
        new MutationParameter(args, List.of("bitFlip"));

    RealParameter mutationProbabilityFactor = new RealParameter("mutationProbabilityFactor", args,
        0.0, 2.0);
    mutationParameter.addGlobalParameter(mutationProbabilityFactor);

    offspringPopulationSizeParameter = new IntegerParameter("offspringPopulationSize", args, 1,
        400);

    variationParameter =
        new VariationCSOParameter(args, List.of("crossoverAndMutationVariation"));
    variationParameter.addGlobalParameter(offspringPopulationSizeParameter);
    variationParameter.addSpecificParameter("crossoverAndMutationVariation", crossoverParameter);
    variationParameter.addSpecificParameter("crossoverAndMutationVariation", mutationParameter);
  }

  private void selection(String[] args) {
    selectionParameter = new SelectionParameter<>(args, Arrays.asList("tournament", "random"));
    IntegerParameter selectionTournamentSize =
        new IntegerParameter("selectionTournamentSize", args, 2, 10);
    selectionParameter.addSpecificParameter("tournament", selectionTournamentSize);
  }

  private void createInitialSolution(String[] args) {
    createInitialSolutionsParameter =
        new CreateInitialCSOSolutionsParameter(
            args, Arrays.asList("random"));
  }

  private void algorithmResult(String[] args) {
    algorithmResultParameter =
        new CategoricalParameter("algorithmResult", args, List.of("externalArchive", "population"));
    populationSizeWithArchiveParameter = new IntegerParameter("populationSizeWithArchive", args, 10,
        200);
    externalArchiveParameter = new ExternalArchiveParameter<>(args,
        List.of("crowdingDistanceArchive"));
    algorithmResultParameter.addSpecificParameter(
        "externalArchive", populationSizeWithArchiveParameter);

    algorithmResultParameter.addSpecificParameter(
        "externalArchive", externalArchiveParameter);
  }

  /**
   * Creates an instance of NSGA-II from the parsed parameters
   *
   * @return
   */
  public EvolutionaryAlgorithm<BinaryCSOSolution> create() {
    Problem<BinaryCSOSolution> problem = new StaticCSO("main.properties", "LL", 0) ;
    Archive<BinaryCSOSolution> archive = null;

    if (algorithmResultParameter.getValue().equals("externalArchive")) {
      externalArchiveParameter.setSize(populationSizeParameter.getValue());
      archive = externalArchiveParameter.getParameter();
      populationSizeParameter.setValue(populationSizeWithArchiveParameter.getValue());
    }

    List<Double> referencePoint = List.of(0.01, -1400.0);
    Ranking<BinaryCSOSolution> ranking = new FastNonDominatedSortRanking<>(new GDominanceComparator<>(referencePoint));
    DensityEstimator<BinaryCSOSolution> densityEstimator = new CrowdingDistanceDensityEstimator<>();
    MultiComparator<BinaryCSOSolution> rankingAndCrowdingComparator =
        new MultiComparator<>(
            Arrays.asList(
                Comparator.comparing(ranking::getRank),
                Comparator.comparing(densityEstimator::getValue).reversed()));

    SolutionsCreation<BinaryCSOSolution> initialSolutionsCreation =
        createInitialSolutionsParameter.getParameter((BinaryCSOProblem) problem,
            populationSizeParameter.getValue());

    mutationParameter.addNonConfigurableParameter("numberOfBitsInASolution", ((StaticCSO) problem).getTotalNumberOfActivableCells());
    Variation<BinaryCSOSolution> variation = (Variation<BinaryCSOSolution>)variationParameter.getBinarySolutionParameter();

    Selection<BinaryCSOSolution> selection =
        selectionParameter.getParameter(
            variation.getMatingPoolSize(), rankingAndCrowdingComparator);

    Evaluation<BinaryCSOSolution> evaluation ;
    if (algorithmResultParameter.getValue().equals("externalArchive")) {
      evaluation = new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }

    RankingAndDensityEstimatorPreference<BinaryCSOSolution> preferenceForReplacement = new RankingAndDensityEstimatorPreference<>(
        ranking, densityEstimator);
    Replacement<BinaryCSOSolution> replacement =
        new RankingAndDensityEstimatorReplacement<>(preferenceForReplacement,
            RemovalPolicy.ONE_SHOT);

    Termination termination =
        new TerminationByEvaluations(maximumNumberOfEvaluationsParameter.getValue());

    class EvolutionaryAlgorithmWithArchive extends EvolutionaryAlgorithm<BinaryCSOSolution> {
      private Archive<BinaryCSOSolution> archive ;
      /**
       * Constructor
       *
       * @param name                      Algorithm name
       * @param initialPopulationCreation
       * @param evaluation
       * @param termination
       * @param selection
       * @param variation
       * @param replacement
       */
      public EvolutionaryAlgorithmWithArchive(String name,
          SolutionsCreation<BinaryCSOSolution> initialPopulationCreation,
          Evaluation<BinaryCSOSolution> evaluation, Termination termination,
          Selection<BinaryCSOSolution> selection, Variation<BinaryCSOSolution> variation,
          Replacement<BinaryCSOSolution> replacement,
          Archive<BinaryCSOSolution> archive) {
        super(name, initialPopulationCreation, evaluation, termination, selection, variation,
            replacement);
        this.archive = archive ;
      }

      @Override
      public List<BinaryCSOSolution> getResult() {
        return archive.solutions() ;
      }
    }

    if (algorithmResultParameter.getValue().equals("externalArchive")) {
      return new EvolutionaryAlgorithmWithArchive(
          "NSGA-II",
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement,
          archive) ;

    } else {
      return new EvolutionaryAlgorithm<>(
          "NSGA-II",
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement);
    }
  }

  public static void print(List<Parameter<?>> parameterList) {
    parameterList.forEach(System.out::println);
  }
}
