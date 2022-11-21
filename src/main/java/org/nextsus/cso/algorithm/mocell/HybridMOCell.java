package org.nextsus.cso.algorithm.mocell;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.algorithm.multiobjective.mocell.MOCell;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.nextsus.cso.problem.StaticCSO;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.legacy.front.Front;
import org.uma.jmetal.util.legacy.front.impl.ArrayFront;
import org.uma.jmetal.util.neighborhood.Neighborhood;

import java.util.List;

public class HybridMOCell<S extends Solution<?>> extends MOCell<S> {
    protected Front referenceFront;

    public HybridMOCell(Problem<S> problem, int maxEvaluations, int populationSize, BoundedArchive<S> archive, Neighborhood<S> neighborhood, CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator, SelectionOperator<List<S>, S> selectionOperator, SolutionListEvaluator<S> evaluator) {
        super(problem, maxEvaluations, populationSize, archive, neighborhood, crossoverOperator, mutationOperator, selectionOperator, evaluator);

        referenceFront = new ArrayFront();
    }

    @Override
    protected List<S> evaluatePopulation(List<S> population) {
        for (S s : population)
            ((StaticCSO) problem).intelligentSwitchOff((BinaryCSOSolution) s);

        return evaluator.evaluate(population, getProblem());
    }

    @Override
    public String getName() {
        return "HybridMOCell";
    }
}
