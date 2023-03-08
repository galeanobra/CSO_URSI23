package org.nextsus.cso.ela.neighborhood;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.List;

public abstract class Neighborhood {
    protected Problem<BinaryCSOSolution> problem;
    protected double percentage;

    public Neighborhood(Problem<BinaryCSOSolution> problem, double percentage) {
        this.problem = problem;
        this.percentage = percentage;
    }

    public abstract List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution);

    public List<BinarySet> generateAllBinaryStrings(List<BinarySet> list, BinarySet b, int i) {
        if (i == b.getBinarySetLength()) {
            BinarySet bCopy = new BinarySet(b.getBinarySetLength());
            for (int j = 0; j < bCopy.getBinarySetLength(); j++)
                bCopy.set(j, b.get(j));
            list.add(bCopy);
            return list;
        }

        b.set(i, false);
        generateAllBinaryStrings(list, b, i + 1);

        b.set(i, true);
        generateAllBinaryStrings(list, b, i + 1);

        return list;
    }

    public enum NeighborhoodType {
        Hamming, Tower, BS, Sector, Cell
    }
}
