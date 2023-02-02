package org.nextsus.cso.operator.crossover;

import org.nextsus.cso.model.UDN;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * Rectangular Geographic Crossover.
 */
public class RGX<T> implements CrossoverOperator<BinaryCSOSolution> {

    protected double probability;
    //    protected int[] range;
    protected UDN udn;
    protected int radius;
    protected final JMetalRandom randomNumberGenerator = JMetalRandom.getInstance();

    public RGX(double probability, int radius, UDN udn) {
        this.probability = probability;
//        this.range = range;
        this.radius = radius;
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

    public List<BinaryCSOSolution> doCrossover(List<BinaryCSOSolution> s) {

        int[] center = new int[]{randomNumberGenerator.nextInt(0, udn.gridPointsX - 1), randomNumberGenerator.nextInt(0, udn.gridPointsY - 1)};
//        int radius = randomNumberGenerator.nextInt(range[0] / (int) udn.getInterpointSeparation(), range[1] / (int) udn.getInterpointSeparation());

        List<Integer> positions = getPositions(center, radius);

        BinaryCSOSolution parent1 = s.get(0).copy();
        BinaryCSOSolution parent2 = s.get(1).copy();

        BinarySet parent1Vector = parent1.variables().get(0);
        BinarySet parent2Vector = parent2.variables().get(0);
        BinarySet offspring1Vector = parent1.variables().get(0);
        BinarySet offspring2Vector = parent2.variables().get(0);

        for (int i : positions) {
            offspring1Vector.set(i, parent2Vector.get(i));
            offspring2Vector.set(i, parent1Vector.get(i));
        }

        List<BinaryCSOSolution> offspring = new ArrayList<>(2);
        offspring.add(new BinaryCSOSolution(offspring1Vector));
        offspring.add(new BinaryCSOSolution(offspring2Vector));

        return offspring;
    }

    public List<Integer> getPositions(int[] center, int radius) {
        List<Integer> positions = new ArrayList<>();
        int[] min = new int[]{Math.min(0, center[0] - radius), Math.min(0, center[1] - radius)};
        int[] max = new int[]{Math.max(0, center[0] + radius), Math.max(0, center[1] + radius)};

        for (int i = 0; i < udn.getCellOrder().size(); i++) {
            int[] pos = udn.getCellOrder().get(i);

            if (pos[0] <= max[0] && pos[0] >= min[0] && pos[1] <= max[1] && pos[1] >= min[1]) positions.add(i);
        }

        return positions;
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
