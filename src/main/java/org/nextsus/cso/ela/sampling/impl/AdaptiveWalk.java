package org.nextsus.cso.ela.sampling.impl;

import org.nextsus.cso.ela.sampling.Walk;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.comparator.dominanceComparator.DominanceComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

public class AdaptiveWalk extends Walk {

    protected int maxWalkLength;
    protected boolean plo;

    public AdaptiveWalk(Problem<BinaryCSOSolution> problem, int maxWalkLength, int numberOfBits) {
        this.problem = problem;
        steps = 0;
        this.maxWalkLength = maxWalkLength;
        selectionOperator = new RandomSelection<>();
        this.numberOfBits = numberOfBits;
        walk = new ArrayList<>();
        plo = false;
    }

    @Override
    public List<BinaryCSOSolution> execute() {
        // Create and evaluate a solution
        BinaryCSOSolution current = new BinaryCSOSolution(List.of(numberOfBits), problem.numberOfObjectives());
        problem.evaluate(current);
        walk.add(current);

        System.out.println("Steps = " + steps + " - Objectives = " + Arrays.toString(current.objectives()));


        // Adaptive walk
        while (steps < maxWalkLength || plo) {
            // Get an improved neighbor
            current = getImprovedNeighbor(current);

            // If null, the solution is a PLO
            if (current != null) {
                walk.add(current);
                steps++;
                System.out.println("Steps = " + steps + " - Objectives = " + Arrays.toString(current.objectives()));
            } else {
                plo = true;
                System.out.println("PLO at steps = " + steps + "\n");
            }
        }

        return walk;
    }

    private BinaryCSOSolution getImprovedNeighbor(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighborhood = getNeighborhood(solution);
        List<Integer> permutation = createIntegerPermutation(neighborhood.size());

        DominanceComparator<BinaryCSOSolution> comparator = new DefaultDominanceComparator<>();
        BinaryCSOSolution neighbor = null;
        for (int i : permutation) {
            neighbor = neighborhood.get(i);
            if (comparator.compare(neighbor, solution) < 0) break;
        }

        return neighbor;
    }

    private List<Integer> createIntegerPermutation(int size) {
        List<Integer> integerList = new LinkedList<>();
        IntStream.range(0, size).forEach(integerList::add);
        List<Integer> permutation = new ArrayList<>(size);
        while (!integerList.isEmpty()) {
            int index = JMetalRandom.getInstance().nextInt(0, integerList.size() - 1);
            permutation.add(integerList.get(index));
            integerList.remove(index);
        }
        return permutation;
    }
}
