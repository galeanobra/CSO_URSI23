package org.nextsus.cso.auto;

import org.uma.jmetal.auto.irace.parameterfilegeneration.IraceParameterFileGenerator;

/**
 * Program to generate the irace configuration file for class {@link AutoNSGAIICSO}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AutoNSGAIIIraceParameterFileGenerator {

  public static void main(String[] args) {
    String[] parameters =
        ("--problemName org.cso.auto.problem.StaticCSOProblem "
            + "--referenceFrontFileName ReferenceFront.csv "
            + "--maximumNumberOfEvaluations 25000 "
            + "--algorithmResult population "
            + "--populationSize 100 "
            + "--offspringPopulationSize 100 "
            + "--createInitialSolutions random "
            + "--variation crossoverAndMutationVariation "
            + "--selection tournament "
            + "--selectionTournamentSize 2 "
            + "--crossover singlePoint "
            + "--crossoverProbability 0.9 "
            + "--mutation bitFlip "
            + "--mutationProbabilityFactor 1.0 ")
            .split("\\s+");

    IraceParameterFileGenerator parameterFileGenerator = new IraceParameterFileGenerator() ;
    parameterFileGenerator.generateConfigurationFile(new AutoNSGAIICSO(), parameters) ;
  }
}
