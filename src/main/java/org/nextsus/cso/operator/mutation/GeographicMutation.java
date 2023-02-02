package org.nextsus.cso.operator.mutation;

import org.nextsus.cso.model.UDN;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GeographicMutation<T> implements MutationOperator<BinaryCSOSolution> {

    private double probability;
    private int radius;
    //    private int[] range;
    private UDN udn;
    protected final JMetalRandom randomNumberGenerator = JMetalRandom.getInstance();


    public GeographicMutation(double probability, int radius, UDN udn) {
        this.probability = probability;
//        this.range = range;
        this.radius = radius;
        this.udn = udn;
    }

    public void doMutation(BinaryCSOSolution solution) {
        int[] center = new int[]{randomNumberGenerator.nextInt(0, udn.gridPointsX - 1), randomNumberGenerator.nextInt(0, udn.gridPointsY - 1)};
//        int radius = PseudoRandom.randInt(range[0] / (int) udn.getInterpointSeparation(), range[1] / (int) udn.getInterpointSeparation());

        List<Integer> positions = getPositions(center);

        for (Integer i : positions)
            solution.variables().get(0).flip(i);
    }

    public List<Integer> getPositions(int[] center) {
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
    public BinaryCSOSolution execute(BinaryCSOSolution s) {
        doMutation(s);
        return s;
    }

    @Override
    public double mutationProbability() {
        return probability;
    }
}
