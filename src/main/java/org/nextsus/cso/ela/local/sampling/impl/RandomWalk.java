package org.nextsus.cso.ela.local.sampling.impl;

import org.nextsus.cso.ela.local.sampling.Walk;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;

import java.util.ArrayList;
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

        // Random walk
        while (steps < walkLength) {
            // Get a random neighbor
            current = selectionOperator.execute(computeNeighborhood(current));
            problem.evaluate(current);
            walk.add(current);
            steps++;

            System.out.println("Steps = " + steps + "\n");
        }

        return walk;
    }
}
