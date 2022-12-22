package org.nextsus.cso.auto.parameter;

import java.util.List;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.catalogue.CrossoverParameter;
import org.uma.jmetal.auto.parameter.catalogue.MutationParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class VariationCSOParameter extends CategoricalParameter {
  public VariationCSOParameter(String[] args, List<String> variationStrategies) {
    super("variation", args, variationStrategies);
  }

  public Variation<DoubleSolution> getDoubleSolutionParameter() {
    Variation<DoubleSolution> result;
    int offspringPopulationSize = (Integer)findGlobalParameter("offspringPopulationSize").getValue() ;

    if ("crossoverAndMutationVariation".equals(getValue())) {
      CrossoverParameter crossoverParameter =
          (CrossoverParameter) findSpecificParameter("crossover");
      MutationParameter mutationParameter = (MutationParameter) findSpecificParameter("mutation");

      CrossoverOperator<DoubleSolution> crossoverOperator = crossoverParameter.getDoubleSolutionParameter();
      MutationOperator<DoubleSolution> mutationOperatorOperator =
          mutationParameter.getDoubleSolutionParameter();

      result =
          new CrossoverAndMutationVariation<>(
              offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
    } else {
      throw new JMetalException("Variation component unknown: " + getValue());
    }

    return result;
  }

  public Variation<? extends BinarySolution> getBinarySolutionParameter() {
    Variation<? extends BinarySolution> result;
    int offspringPopulationSize = (Integer)findGlobalParameter("offspringPopulationSize").getValue() ;

    if ("crossoverAndMutationVariation".equals(getValue())) {
      CrossoverParameter crossoverParameter =
          (CrossoverParameter) findSpecificParameter("crossover");
      MutationParameter mutationParameter = (MutationParameter) findSpecificParameter("mutation");

      CrossoverOperator<BinarySolution> crossoverOperator = crossoverParameter.getBinarySolutionParameter();
      MutationOperator<BinarySolution> mutationOperatorOperator =
          mutationParameter.getBinarySolutionParameter();

      result =
          new CrossoverAndMutationVariation<>(
              offspringPopulationSize, crossoverOperator, mutationOperatorOperator);
    } else {
      throw new JMetalException("Variation component unknown: " + getValue());
    }

    return result;
  }

  @Override
  public String getName() {
    return "variation";
  }
}
