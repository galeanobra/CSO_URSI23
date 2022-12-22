package org.nextsus.cso.model;

import org.junit.jupiter.api.Test;
import org.nextsus.cso.operator.BinaryTwoPointCrossover;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BinaryTwoPointCrossoverTest {

    @Test
    void doCrossover() {
        StaticCSO cso = new StaticCSO("main.properties", "LL", 0);

        Random r = new Random(1234567890);

        boolean[] solution1Bool = new boolean[cso.getTotalNumberOfActivableCells()];
        boolean[] solution2Bool = new boolean[cso.getTotalNumberOfActivableCells()];
        for (int i = 0; i < solution1Bool.length; i++) {
            solution1Bool[i] = r.nextBoolean();
            solution2Bool[i] = r.nextBoolean();
        }

        BinaryCSOSolution s1 = new BinaryCSOSolution(cso.getListOfBitsPerVariable(), cso.numberOfObjectives());
        BinaryCSOSolution s2 = new BinaryCSOSolution(cso.getListOfBitsPerVariable(), cso.numberOfObjectives());
        BinarySet bits1 = s1.variables().get(0);
        BinarySet bits2 = s2.variables().get(0);
        for (int i = 0; i < bits1.getBinarySetLength(); i++) {
            bits1.set(i, solution1Bool[i]);
            bits2.set(i, solution2Bool[i]);
        }

        List<BinaryCSOSolution> parents = new ArrayList<>();
        parents.add(s1);
        parents.add(s2);

        CrossoverOperator<BinaryCSOSolution> crossoverOperator = new BinaryTwoPointCrossover<BinaryCSOSolution>(1);

        List<BinaryCSOSolution> offspring = crossoverOperator.execute(parents);

        System.out.println(parents.get(0));
        System.out.println(parents.get(1));
        System.out.println(offspring.get(0));
        System.out.println(offspring.get(1));
    }
}
