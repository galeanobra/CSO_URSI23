package org.nextsus.cso.ela.sampling;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;

import java.util.ArrayList;
import java.util.List;

public abstract class Walk {

    protected Problem<BinaryCSOSolution> problem;
    protected int numberOfBits;
    protected int steps;
    protected int walkLength;
    protected SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selectionOperator;
    protected List<BinaryCSOSolution> walk;

    public abstract List<BinaryCSOSolution> execute();

    protected List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();

        for (int i = 0; i < solution.getNumberOfBits(0); i++) {
            // Generate the new neighbor setting the Hamming distance to 1
            BinaryCSOSolution s = solution.copy();
            s.variables().get(0).flip(i);
            problem.evaluate(s);
            neighbors.add(s);
        }

        return neighbors;
    }
}
