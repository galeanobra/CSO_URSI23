package org.nextsus.cso.ela.sampling.impl;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.ela.neighborhood.impl.*;
import org.nextsus.cso.ela.sampling.Walk;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RandomWalk extends Walk {
    public RandomWalk(Problem<BinaryCSOSolution> problem, int walkLength, int numberOfBits, Neighborhood.NeighborhoodType neighborhoodType) {
        this.problem = problem;
        steps = 0;
        evaluations = 0;
        this.walkLength = walkLength;
        selectionOperator = new RandomSelection<>();
        this.numberOfBits = numberOfBits;
        walk = new ArrayList<>();
        n = switch (neighborhoodType) {
            case Tower -> new TowerNeighborhood(problem, 1.0);
            case BS -> new BSNeighborhood(problem, 1.0);
            case Sector -> new SectorNeighborhood(problem, 1.0);
            case Cell -> new CellNeighborhood(problem, 1.0);
            case Hamming -> new HammingNeighborhood(problem, 1.0);
        };
    }

    @Override
    public List<BinaryCSOSolution> execute() {
        // Create and evaluate a solution
        BinaryCSOSolution current = initialSolution == null ? new BinaryCSOSolution(List.of(numberOfBits), problem.numberOfObjectives()) : initialSolution;
        problem.evaluate(current);
        evaluations++;
        walk.add(current);

//        System.out.println("Steps = " + steps + " - Objectives = " + Arrays.toString(current.objectives()));

        // Random walk
        while (steps < walkLength) {
            // Get a random neighbor
            current = selectionOperator.execute(n.getNeighborhood(current));
            problem.evaluate(current);
            evaluations++;
            walk.add(current);
            steps++;

//            System.out.println("Steps = " + steps + " - Objectives = " + Arrays.toString(current.objectives()));
        }

        return walk;
    }
}
