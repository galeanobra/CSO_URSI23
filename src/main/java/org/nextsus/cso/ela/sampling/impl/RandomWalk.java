package org.nextsus.cso.ela.sampling.impl;

import org.nextsus.cso.ela.sampling.Walk;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RandomWalk extends Walk {
    public RandomWalk(Problem<BinaryCSOSolution> problem, int walkLength, int numberOfBits) {
        this.problem = problem;
        steps = 0;
        this.walkLength = walkLength;
        selectionOperator = new RandomSelection<>();
        this.numberOfBits = numberOfBits;
        walk = new ArrayList<>();
    }

    @Override
    public List<BinaryCSOSolution> execute() {
        // Create and evaluate a solution
        BinaryCSOSolution current = new BinaryCSOSolution(List.of(numberOfBits), problem.numberOfObjectives());
        problem.evaluate(current);
        walk.add(current);

        System.out.println("Steps = " + steps + " - Objectives = " + Arrays.toString(current.objectives()));


        // Random walk
        while (steps < walkLength) {
            // Get a random neighbor
            current = selectionOperator.execute(getNeighborhood(current));
            problem.evaluate(current);
            walk.add(current);
            steps++;

            System.out.println("Steps = " + steps + " - Objectives = " + Arrays.toString(current.objectives()));
        }

        return walk;
    }
}
