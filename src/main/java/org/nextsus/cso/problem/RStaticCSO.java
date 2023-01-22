package org.nextsus.cso.problem;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.binarysolution.impl.DefaultBinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.ArrayList;
import java.util.List;

public class RStaticCSO extends AbstractDoubleProblem {

    private StaticCSO problem;

    public RStaticCSO(StaticCSO problem) {
        this.problem = problem;

        setNumberOfObjectives(problem.getNumberOfObjectives());
        setNumberOfConstraints(problem.getNumberOfConstraints());
        setName("RStaticCSO");

        List<Double> lowerLimit = new ArrayList<>(problem.bits);
        List<Double> upperLimit = new ArrayList<>(problem.bits);

        for (int i = 0; i < problem.bits; i++) {
            lowerLimit.add(0.0);
            upperLimit.add(1.0);
        }

        setVariableBounds(lowerLimit, upperLimit);
    }

    @Override
    public DoubleSolution evaluate(DoubleSolution solution) {
        BinarySet binarySet = new BinarySet(solution.variables().size());
        for (int i = 0; i < solution.variables().size(); i++) {
            binarySet.set(i, solution.variables().get(i) > 0.5);
        }

        BinaryCSOSolution binarySolution = new BinaryCSOSolution(List.of(binarySet.size()), 2);
        binarySolution.variables().set(0, binarySet);

        binarySolution = evaluateBinary(binarySolution);
        solution.objectives()[0] = binarySolution.objectives()[0];
        solution.objectives()[1] = binarySolution.objectives()[1];
        return solution;
    }

    public BinaryCSOSolution evaluateBinary(BinaryCSOSolution solution) {
        return problem.evaluate(solution);
    }
}
