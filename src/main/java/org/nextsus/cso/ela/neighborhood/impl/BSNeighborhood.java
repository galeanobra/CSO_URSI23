package org.nextsus.cso.ela.neighborhood.impl;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class BSNeighborhood extends Neighborhood {

    public BSNeighborhood(Problem<BinaryCSOSolution> problem, double percentage) {
        super(problem, percentage);
    }

    @Override
    public List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();
        UDN udn = ((StaticCSO) problem).getUDN();

        for (BTS b : udn.getBTSsList()) {
            // Por cada BS, coger la BS del mismo tipo más cercana
            // Obtener todos los vecinos posibles con esa BS más cercana
            BTS closest = udn.getClosestCellByType(b.getPoint(), b.getSectors().get(0).getCells().get(0).getType()).getBTS();

            List<BinarySet> neighborsBinarySet = generateAllBinaryStrings(new ArrayList<>(), new BinarySet(closest.getNumberOfInstalledCells()), 0);

            int i = b.getSectors().get(0).getCells().get(0).getID();
            for (BinarySet binarySet : neighborsBinarySet) {
                if (JMetalRandom.getInstance().nextDouble() <= percentage) {
                    BinaryCSOSolution neighbor = solution.copy();
                    for (int j = 0; j < binarySet.getBinarySetLength(); j++)
                        neighbor.variables().get(0).set(i + j, binarySet.get(j));
                    neighbors.add(neighbor);
                }
            }
        }

        return neighbors;
    }
}
