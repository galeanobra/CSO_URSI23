package org.nextsus.cso.operator.crossover;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.util.errorchecking.Check;

public class BinaryTwoPointCrossoverMOEAD<T> extends BinaryTwoPointCrossover<BinaryCSOSolution> {

    public BinaryTwoPointCrossoverMOEAD(double probability) {
        super(probability);
    }

    @Override
    protected List<BinaryCSOSolution> doCrossover(List<BinaryCSOSolution> s) {
        BinaryCSOSolution mom = s.get(randomNumberGenerator.nextInt(0, 1));
        BinaryCSOSolution dad = s.get(2);

        Check.that(mom.variables().size() == dad.variables().size(), "The 2 parents doesn't have the same number of variables");

        BinaryCSOSolution girl = mom.copy();
        BinaryCSOSolution boy = dad.copy();
        boolean swap = false;

        Check.that(mom.variables().get(0).getBinarySetLength() >= 2, "The number of crossovers is higher than the number of bits");
        int[] crossoverPoints = new int[2];
        for (int i = 0; i < crossoverPoints.length; i++) {
            crossoverPoints[i] = randomNumberGenerator.nextInt(0, mom.variables().get(0).getBinarySetLength() - 1);
        }

        for (int i = 0; i < mom.variables().get(0).getBinarySetLength(); i++) {
            if (swap) {
                boy.variables().get(0).set(i, mom.variables().get(0).get(i));
                girl.variables().get(0).set(i, dad.variables().get(0).get(i));
            }

            if (ArrayUtils.contains(crossoverPoints, i)) {
                swap = !swap;
            }
        }

        List<BinaryCSOSolution> result = new ArrayList<>();
        result.add(girl);
        result.add(boy);
        return result;
    }

    @Override
    public int numberOfRequiredParents() {
        return 3;
    }
}