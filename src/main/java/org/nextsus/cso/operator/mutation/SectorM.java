package org.nextsus.cso.operator.mutation;

import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.cells.Sector;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class SectorM<T> implements MutationOperator<BinaryCSOSolution> {
    protected double probability;
    protected int n;
    protected UDN udn;
    protected final JMetalRandom randomNumberGenerator = JMetalRandom.getInstance();

    public SectorM(double probability, int n, UDN udn) {
        this.probability = probability;
        this.n = n;
        this.udn = udn;
    }

    @Override
    public BinaryCSOSolution execute(BinaryCSOSolution s) {
        doMutation(s);
        return s;
    }

    public BinaryCSOSolution doMutation(BinaryCSOSolution solution) {
        List<Sector> sectors = udn.getSectorsList();
        List<Integer> sectorIndex = new ArrayList<>(n);

        int counter = 0;
        while (counter < n) {
            int tmp = randomNumberGenerator.nextInt(0, sectors.size() -1);
            if (!sectorIndex.contains(tmp)) {
                sectorIndex.add(tmp);
                counter++;
            }
        }

        List<Integer> cellIDs = new ArrayList<>();
        for (int i : sectorIndex) {
            for (Cell c : sectors.get(i).getCells()) {
                cellIDs.add(c.getID());
            }
        }

        for (int i : cellIDs) {
            solution.variables().get(0).set(i, !solution.variables().get(0).get(i));
        }

        return solution;
    }

    @Override
    public double mutationProbability() {
        return probability;
    }
}
