package org.nextsus.cso.algorithm.smsemoa;

import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.algorithm.multiobjective.mocell.MOCell;
import org.uma.jmetal.algorithm.multiobjective.smsemoa.SMSEMOA;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.legacy.front.Front;
import org.uma.jmetal.util.legacy.front.impl.ArrayFront;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.Hypervolume;
import org.uma.jmetal.util.neighborhood.Neighborhood;

import java.util.Comparator;
import java.util.List;

public class HybridSMSEMOA<S extends Solution<?>> extends SMSEMOA<S> {
    protected Front referenceFront;

    public HybridSMSEMOA(Problem<S> problem, int maxEvaluations, int populationSize, double offset, CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator, SelectionOperator<List<S>, S> selectionOperator, Comparator<S> dominanceComparator, Hypervolume<S> hypervolumeImplementation) {
        super(problem, maxEvaluations, populationSize, offset, crossoverOperator, mutationOperator, selectionOperator, dominanceComparator, hypervolumeImplementation);

        referenceFront = new ArrayFront();
    }

    @Override
    protected List<S> evaluatePopulation(List<S> population) {
        for (S s : population) {
            ((StaticCSO) problem).intelligentSwitchOff((BinaryCSOSolution) s);
            getProblem().evaluate(s);
        }

        return population;
    }

    @Override
    public String getName() {
        return "HybridSMS-EMOA";
    }
}
