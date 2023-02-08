package org.nextsus.cso.operator.mutation;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.random.RandomGenerator;

public class BiasedBitFlipMutation extends BitFlipMutation<BinaryCSOSolution> {
    protected double bias;

    public BiasedBitFlipMutation(double mutationProbability, double bias) {
        super(mutationProbability);
        this.bias = bias;
    }

    @Override
    public void doMutation(double probability, BinaryCSOSolution solution) {
        for (int i = 0; i < solution.variables().size(); i++) {
            for (int j = 0; j < solution.variables().get(i).getBinarySetLength(); j++) {
                if (JMetalRandom.getInstance().nextDouble() <= probability) {
                    boolean bit = solution.variables().get(i).get(j);
                    if (bit && JMetalRandom.getInstance().nextDouble() < bias || !bit && JMetalRandom.getInstance().nextDouble() >= bias)
                        solution.variables().get(i).flip(j);
                }
            }
        }
    }
}
