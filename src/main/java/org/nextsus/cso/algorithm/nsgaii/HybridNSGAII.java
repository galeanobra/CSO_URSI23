package org.nextsus.cso.algorithm.nsgaii;

import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.legacy.front.Front;
import org.uma.jmetal.util.legacy.front.impl.ArrayFront;
import org.uma.jmetal.util.measure.Measurable;
import org.uma.jmetal.util.measure.MeasureManager;
import org.uma.jmetal.util.measure.impl.BasicMeasure;
import org.uma.jmetal.util.measure.impl.CountingMeasure;
import org.uma.jmetal.util.measure.impl.DurationMeasure;
import org.uma.jmetal.util.measure.impl.SimpleMeasureManager;

import java.util.Comparator;
import java.util.List;

public class HybridNSGAII<S extends Solution<?>> extends NSGAII<S> implements Measurable {

    protected SimpleMeasureManager measureManager;
    protected BasicMeasure<List<S>> solutionListMeasure;
    protected CountingMeasure evaluations;
    protected DurationMeasure durationMeasure;

    protected Front referenceFront;

    public HybridNSGAII(Problem<S> problem, int maxEvaluations, int populationSize, int matingPoolSize, int offspringPopulationSize, CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator, SelectionOperator<List<S>, S> selectionOperator, Comparator<S> dominanceComparator, SolutionListEvaluator<S> evaluator) {
        super(problem, maxEvaluations, populationSize, matingPoolSize, offspringPopulationSize, crossoverOperator, mutationOperator, selectionOperator, new DefaultDominanceComparator<S>(), evaluator);

        referenceFront = new ArrayFront();

        initMeasures();
    }

    @Override
    protected void initProgress() {
        evaluations.reset(getMaxPopulationSize());
    }

    @Override
    protected void updateProgress() {
        evaluations.increment(getMaxPopulationSize());
        solutionListMeasure.push(getPopulation());
    }

    @Override
    protected boolean isStoppingConditionReached() {
        return evaluations.get() >= maxEvaluations;
    }

    @Override
    public void run() {
        durationMeasure.reset();
        durationMeasure.start();
        super.run();
        durationMeasure.stop();
    }

    @Override
    protected List<S> evaluatePopulation(List<S> population) {
        if (problem.name().equals("StaticCSO"))
            for (S s : population)
                ((StaticCSO) problem).intelligentSwitchOff((BinaryCSOSolution) s);

        return evaluator.evaluate(population, getProblem());
    }

    /* Measures code */
    private void initMeasures() {
        durationMeasure = new DurationMeasure();
        evaluations = new CountingMeasure(0);
        solutionListMeasure = new BasicMeasure<>();

        measureManager = new SimpleMeasureManager();
        measureManager.setPullMeasure("currentExecutionTime", durationMeasure);
        measureManager.setPullMeasure("currentEvaluation", evaluations);

        measureManager.setPushMeasure("currentPopulation", solutionListMeasure);
        measureManager.setPushMeasure("currentEvaluation", evaluations);
    }

    @Override
    public MeasureManager getMeasureManager() {
        return measureManager;
    }

    public CountingMeasure getEvaluations() {
        return evaluations;
    }

    @Override
    public String getName() {
        return "HybridNSGAII";
    }
}
