package org.nextsus.cso.ela.neighborhood.impl;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class CellNeighborhood extends Neighborhood {

    public CellNeighborhood(Problem<BinaryCSOSolution> problem, double percentage) {
        super(problem, percentage);
    }

    public List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();
        UDN udn = ((StaticCSO) problem).getUDN();

        for (double d : udn.getCells().keySet()) {
            for (Cell c : udn.cells.get(d)) {
                if (JMetalRandom.getInstance().nextDouble() <= percentage) {
                    Cell closest = udn.getClosestCellByType(c.getBTS().getPoint(), c.getType());
                    BinaryCSOSolution neighbor = solution.copy();
                    neighbor.variables().get(0).set(closest.getID(), !neighbor.variables().get(0).get(closest.getID()));
                    neighbors.add(neighbor);
                }
            }
        }

        return neighbors;
    }
}
