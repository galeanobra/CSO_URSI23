package org.nextsus.cso.ela.sampling;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.RandomSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

public class PLS {
    protected Problem<BinaryCSOSolution> problem;
    protected int evaluations;
    protected int maxEvaluations;
    protected int populationSize;
    protected Archive<BinaryCSOSolution> archive;
    protected SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selectionOperator;
    protected MutationOperator<BinaryCSOSolution> mutationOperator;
    protected int iterations;
    protected int numberOfBits;


    /**
     * Constructor
     *
     * @param problem The problem to solve
     */
    public PLS(Problem<BinaryCSOSolution> problem, int maxEvaluations, double mutationProbability, int numberOfBits) {
        this.problem = problem;
        evaluations = 0;
        this.maxEvaluations = maxEvaluations;
        archive = new NonDominatedSolutionListArchive<>();
        selectionOperator = new RandomSelection<>();
        mutationOperator = new BitFlipMutation<>(mutationProbability);
        iterations = 0;
        this.numberOfBits = numberOfBits;
    }

    public List<BinaryCSOSolution> execute() {
        // Create and evaluate a solution
        BinaryCSOSolution current = new BinaryCSOSolution(List.of(numberOfBits), problem.numberOfObjectives());
        problem.evaluate(current);
        evaluations++;

        // Add to the archive
        archive.add(current);

        // Generations
        while (evaluations < maxEvaluations) {
            //Get an unmarked solution from the archive, which marked before returning
            current = getUnmarked();

            System.out.println("Evaluations = " + evaluations + " - Marked solutions = " + numberOfMarkedSolutions() + " - Archive = " + archive.size() + " - Iterations = " + iterations);

            if (current == null) {
                iterations++;
                // All solutions have been processed. Then, choose a random solution from the archive
                System.out.println("Generating a new solution by mutation");
                current = selectionOperator.execute(archive.solutions()).copy();
                current.unmark();

                // Mutate
                mutationOperator.execute(current);
                problem.evaluate(current);
                evaluations++;

                // Insert into the archive and continue
                archive.add(current);
            }

            // Compute the neighborhood of the solution
            List<BinaryCSOSolution> neighbors = computeNeighborhood(current);
//            System.out.println(neighbors.size() + " neighbors");
            // Evaluate the solutions and add them to the archive
            for (BinaryCSOSolution n : neighbors) {
                problem.evaluate(n);
                evaluations++;
                archive.add(n);
            }
//            for (BinaryCSOSolution s : archive.solutions())
//                System.out.println(s);
            System.out.println();
        }

        System.out.println("Iterations = " + iterations);
        System.out.println("Archive = " + archive.size());

        return archive.solutions();
    }

    public List<BinaryCSOSolution> computeNeighborhood(BinaryCSOSolution current) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();

        for (int i = 0; i < current.getNumberOfBits(0); i++) {
            // Generate the new neighbor
            BinaryCSOSolution s = current.copy();
            s.unmark();

            // Set Hamming distance to 1
            s.variables().get(0).flip(i);

            // Add to thje solution set
            neighbors.add(s);
        }

        return neighbors;
    }

    private BinaryCSOSolution getUnmarked() {
        BinaryCSOSolution s = null;
        // Creates the permutation
        List<Integer> p = createIntegerPermutation();

        for (Integer integer : p) {
            if (!archive.get(integer).isMarked()) {
                s = archive.get(integer);
                break;
            }
        }

        if (s == null) return null;
        else {
            archive.solutions().remove(s);
            s.mark();
            problem.evaluate(s);
            archive.add(s);
            return s;
        }
    }

    private List<Integer> createIntegerPermutation() {
        List<Integer> integerList = new LinkedList<>();
        IntStream.range(0, archive.size()).forEach(integerList::add);
        List<Integer> permutation = new ArrayList<>(archive.size());
        while (!integerList.isEmpty()) {
            int index = JMetalRandom.getInstance().nextInt(0, integerList.size() - 1);
            permutation.add(integerList.get(index));
            integerList.remove(index);
        }
        return permutation;
    }

    private int numberOfMarkedSolutions() {
        int count = 0;
        for (BinaryCSOSolution s : archive.solutions())
            if (s.isMarked()) count++;
        return count;
    }

    private void print() {
        System.out.println("Archive content: ");
        for (BinaryCSOSolution s : archive.solutions())
            System.out.print(s.isMarked() ? "X " : "- ");
        System.out.println();
    }

    private int countActives(BinaryCSOSolution current) {
        return current.variables().get(0).cardinality();
    }
}
