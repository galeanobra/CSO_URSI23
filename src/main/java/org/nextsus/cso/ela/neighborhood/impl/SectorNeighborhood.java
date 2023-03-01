package org.nextsus.cso.ela.neighborhood.impl;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.Sector;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.ArrayList;
import java.util.List;

public class SectorNeighborhood extends Neighborhood {

    public SectorNeighborhood(Problem<BinaryCSOSolution> problem) {
        super(problem);
    }

    public List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();
        UDN udn = ((StaticCSO) problem).getUDN();

        for (Sector s : udn.getSectorsList()) {
            // TODO Falta elegir el sector que más apunta a s
            Sector closest = udn.getClosestCellByType(s.getBTS().getPoint(), s.getCells().get(0).getType()).getSector();

            List<BinarySet> neighborsBinarySet = generateAllBinaryStrings(new ArrayList<>(), new BinarySet(closest.getCells().size()), 0);

            int i = s.getCells().get(0).getID();
            for (BinarySet b : neighborsBinarySet) {
                BinaryCSOSolution neighbor = solution.copy();
                for (int j = 0; j < b.getBinarySetLength(); j++)
                    neighbor.variables().get(0).set(i + j, b.get(j));
                problem.evaluate(neighbor);
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }
}
