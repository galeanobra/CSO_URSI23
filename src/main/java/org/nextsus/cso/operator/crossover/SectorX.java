package org.nextsus.cso.operator.crossover;

import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.cells.Sector;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class SectorX<T> implements CrossoverOperator<BinaryCSOSolution> {
    protected double probability;
    protected int n;
    protected UDN udn;
    protected final JMetalRandom randomNumberGenerator = JMetalRandom.getInstance();

    public SectorX(double probability, int n, UDN udn) {
        this.probability = probability;
        this.n = n;
        this.udn = udn;
    }

    @Override
    public List<BinaryCSOSolution> execute(List<BinaryCSOSolution> s) {
        Check.that(numberOfRequiredParents() == s.size(), "Point Crossover requires + " + numberOfRequiredParents() + " parents, but got " + s.size());

        if (randomNumberGenerator.nextDouble() < probability) {
            return doCrossover(s);
        } else {
            return s;
        }
    }

    public List<BinaryCSOSolution> doCrossover(List<BinaryCSOSolution> parents) {
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

        BinaryCSOSolution parent1 = parents.get(0).copy();
        BinaryCSOSolution parent2 = parents.get(1).copy();

        BinarySet parent1Vector = parent1.variables().get(0);
        BinarySet parent2Vector = parent2.variables().get(0);
        BinarySet offspring1Vector = parent1.variables().get(0);
        BinarySet offspring2Vector = parent2.variables().get(0);

        for (int i : cellIDs) {
            offspring1Vector.set(i, parent2Vector.get(i));
            offspring2Vector.set(i, parent1Vector.get(i));
        }

        List<BinaryCSOSolution> offspring = new ArrayList<>(2);
        offspring.add(new BinaryCSOSolution(offspring1Vector));
        offspring.add(new BinaryCSOSolution(offspring2Vector));

        return offspring;
    }

    @Override
    public double crossoverProbability() {
        return probability;
    }

    @Override
    public int numberOfRequiredParents() {
        return 2;
    }

    @Override
    public int numberOfGeneratedChildren() {
        return 2;
    }


}
