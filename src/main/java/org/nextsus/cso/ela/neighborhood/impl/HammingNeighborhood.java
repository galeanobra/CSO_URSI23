package org.nextsus.cso.ela.neighborhood.impl;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class HammingNeighborhood extends Neighborhood {
    public HammingNeighborhood(Problem<BinaryCSOSolution> problem, double percentage) {
        super(problem, percentage);
    }

    @Override
    public List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();

        for (int i = 0; i < solution.getNumberOfBits(0); i++) {
            if (JMetalRandom.getInstance().nextDouble() <= percentage) {
                BinaryCSOSolution neighbor = solution.copy();
                neighbor.variables().get(0).flip(i);
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }
}
