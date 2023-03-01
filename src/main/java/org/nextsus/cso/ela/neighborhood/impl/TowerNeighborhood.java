package org.nextsus.cso.ela.neighborhood.impl;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;

import java.util.List;

public class TowerNeighborhood extends Neighborhood {

    public TowerNeighborhood(Problem<BinaryCSOSolution> problem){
        super(problem);
    }

    @Override
    public List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        return null;
    }


}
