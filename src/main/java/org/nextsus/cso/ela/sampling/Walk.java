package org.nextsus.cso.ela.sampling;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;

import java.util.List;

public abstract class Walk {

    protected Problem<BinaryCSOSolution> problem;
    protected int numberOfBits;
    protected int steps;
    protected int walkLength;

    protected int evaluations;
    protected int maxEvaluations;
    protected SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selectionOperator;
    protected List<BinaryCSOSolution> walk;

    protected Neighborhood n;

    protected BinaryCSOSolution initialSolution;

    public abstract List<BinaryCSOSolution> execute();

    public void setInitialSolution(BinaryCSOSolution initialSolution) {
        this.initialSolution = initialSolution;
    }
}
