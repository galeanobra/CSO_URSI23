package org.nextsus.cso.algorithm.sparseea;

import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.pseudorandom.RandomGenerator;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SparseEA.
 * Y. Tian, X. Zhang, C. Wang and Y. Jin, "An Evolutionary Algorithm
 * for Large-Scale Sparse Multiobjective Optimization Problems," in
 * IEEE Transactions on Evolutionary Computation, vol. 24, no. 2,
 * pp. 380-393, April 2020.
 */
public class HybridSparseEA<S extends Solution<?>> {

    //    private final List<Solution> tabu;
//    private int tabuSize;
    protected Problem<S> problem;
    protected int maxEvaluations;
    protected int populationSize;
    protected double crossoverProb;
    protected double mutationProb;
    protected SelectionOperator<List<S>, S> selectionOperator;
    protected int numberOfBits;

    protected int evaluations;

    private RandomGenerator<Double> randomGenerator;

    protected Comparator<S> dominanceComparator;

    /**
     * Constructor
     *
     * @param problem Problem to solve
     */
    public HybridSparseEA(Problem<S> problem, int maxEvalutaions, int populationSize, double crossoverProb, double mutationProb, SelectionOperator<List<S>, S> selectionOperator, int numberOfBits) {
//        tabu = new ArrayList<>();
//        tabuSize = 5;
        this.problem = problem;
        this.maxEvaluations = maxEvalutaions;
        this.populationSize = populationSize;
        this.crossoverProb = crossoverProb;
        this.mutationProb = mutationProb;
        this.selectionOperator = selectionOperator;
        this.numberOfBits = numberOfBits;

        this.evaluations = 0;

        this.dominanceComparator = new DefaultDominanceComparator<>();
    }

    /**
     * Runs the SparseEA algorithm.
     *
     * @return a <code>SolutionSet</code> that is a set of non dominated
     * solutions as a result of the algorithm execution
     */
    public List<S> execute() {
        List<BinaryCSOSolution> population;
        List<BinaryCSOSolution> offspringPopulation;
        List<BinaryCSOSolution> union;

        int[] score;                        // Score of the decision variables
        int d;                              // Number of decision variables
        boolean[][] dec;                    // Denote the decision variables TODO double if Real optimization
        boolean[][] mask;                   // Denote the mask for decs

        CrowdingDistanceDensityEstimator<BinaryCSOSolution> distance = new CrowdingDistanceDensityEstimator<>();

//        tabuSize = (Integer) getInputParameter("tabuSize");

        // Initialize the variables
        d = this.numberOfBits;      // Number of decision variables
        mask = new boolean[d][d];    // dec x mask

        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) {
                mask[i][j] = i == j;
            }
        }

        // A population whose i-th solution is generated by de i-th rows of dec and mask (i-th row of variables)
        List<BinaryCSOSolution> q = generatePopulation(d, d, mask);  // dec = dec x mask because mask = identity matrix

        // Return non-dominated fronts
        Ranking<S> ranking = (FastNonDominatedSortRanking<S>) new FastNonDominatedSortRanking<>((Comparator<BinaryCSOSolution>) this.dominanceComparator).compute(q);
        score = new int[d];

        // The score is the rank of the subfront
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < ranking.getNumberOfSubFronts(); j++) {
                if (ranking.getSubFront(j).contains(q.get(i))) {
                    score[i] = j;
                    break;
                }
            }
        }

        mask = new boolean[populationSize][d];
        for (boolean[] booleans : mask)
            Arrays.fill(booleans, false);

        for (int i = 0; i < populationSize; i++) {
            for (int j = 0; j < JMetalRandom.getInstance().nextDouble() * d; j++) {
                int m = JMetalRandom.getInstance().nextInt(0, d - 1);
                int n = JMetalRandom.getInstance().nextInt(0, d - 1);

                if (score[m] < score[n]) mask[i][m] = true;
                else mask[i][n] = true;
            }
        }

        // A population whose i-th solution is generated by de i-th rows of dec and mask
        population = generatePopulation(populationSize, d, mask);   // mask = dec x mask because dec = matrix of ones

        distance.compute(population);

        // Generations
        while (this.evaluations < this.maxEvaluations) {
            List<BinaryCSOSolution> _population = new ArrayList<>(2 * populationSize);   // P' -> 2 * N population
            mask = new boolean[populationSize * 2][d];
            for (int i = 0; i < 2 * populationSize; i++) {                              // Fill _population with 2N parents from population
                BinaryCSOSolution s = (BinaryCSOSolution) this.selectionOperator.execute((List<S>) population);
                _population.add(s);

                for (int j = 0; j < d; j++)
                    mask[i][j] = s.variables().get(0).get(j);
            }

            // Union
            Set<BinaryCSOSolution> set = new HashSet<>();
            set.addAll(population);
            set.addAll(variation(_population, population, score, mask));

            population = new ArrayList<>(set);

            // Remove duplicates solutions
            population = population.stream().distinct().collect(Collectors.toList());

            ranking = new FastNonDominatedSortRanking<>(this.dominanceComparator).compute((List<S>) population);
            population.clear();

            int remain = populationSize;
            int index = 0;

            // Obtain the next front
            List<BinaryCSOSolution> front = (List<BinaryCSOSolution>) ranking.getSubFront(index);

            while ((remain > 0) && (remain >= front.size())) {
                //Assign crowding distance to individuals
                distance.compute(front);
                //Add the individuals of this front
                population.addAll(front);

                remain = remain - front.size();

                //Obtain the next front
                index++;
                if (remain > 0) front = (List<BinaryCSOSolution>) ranking.getSubFront(index);
            }

            // Remain is less than front(index).size, insert only the best one
            if (remain > 0) {  // front contains individuals to insert
                distance.compute(front);
                front.sort(new RankingAndCrowdingDistanceComparator<>());
                for (int k = 0; k < remain; k++)
                    population.add(front.get(k));
            }
        }

        ranking = new FastNonDominatedSortRanking<>(this.dominanceComparator).compute((List<S>) population);
        for (S binarySolution : ranking.getSubFront(0)) problem.evaluate(binarySolution);
        evaluations += ranking.getSubFront(0).size();

        return ranking.getSubFront(0);
//        return population;
    }

    /**
     * Generate a population whose i-th solution is generated by de i-th rows of dec and mask (i-th row of variables)
     *
     * @param populationSize Population size
     * @param d              Number of decision variables
     * @param variables      dec * mask
     * @return New population
     */
    private List<BinaryCSOSolution> generatePopulation(int populationSize, int d, boolean[][] variables) {
        List<BinaryCSOSolution> population = new ArrayList<>(populationSize);

        for (int i = 0; i < populationSize; i++) {
            BinaryCSOSolution s = new BinaryCSOSolution(Collections.singletonList(this.numberOfBits), problem.numberOfObjectives());
            BitSet bits = s.variables().get(0);
            for (int j = 0; j < d; j++)
                bits.set(j, variables[i][j]);

            problem.evaluate((S) s);
//            problem.evaluateConstraints(s);
            population.add(s);
        }

        evaluations += populationSize;

        return population;
    }

    /**
     * Variation of population P'. Algorithm 3 from the paper.
     *
     * @param _population 2 * N parents population
     * @param population  Population
     * @param score       Decision variables scores
     * @param mask        Mask matrix
     * @return Variation of population P' (_population)
     */
    public List<BinaryCSOSolution> variation(List<BinaryCSOSolution> _population, List<BinaryCSOSolution> population, int[] score, boolean[][] mask) {
        // PlatEMO implementation
        boolean[][] pMask = new boolean[mask.length][_population.size() / 2];
        boolean[][] qMask = new boolean[mask.length][_population.size() / 2];
        boolean[][] oMask = new boolean[mask.length][_population.size() / 2];

        for (int i = 0; i < _population.size() / 2; i++) {
            pMask[i] = mask[i];
            qMask[i] = mask[(_population.size() / 2 - 1) + i];

            //TODO
            if (countBool(pMask[i], true) == 0) pMask[i][JMetalRandom.getInstance().nextInt(0, pMask[i].length - 1)] = true;
            if (countBool(qMask[i], true) == 0) qMask[i][JMetalRandom.getInstance().nextInt(0, qMask[i].length - 1)] = true;

            if (countBool(pMask[i], false) == 0) pMask[i][JMetalRandom.getInstance().nextInt(0, pMask[i].length - 1)] = false;
            if (countBool(qMask[i], false) == 0) qMask[i][JMetalRandom.getInstance().nextInt(0, qMask[i].length - 1)] = false;
            //TODO

            oMask[i] = mask[i];
        }

        // Crossover
        for (int i = 0; i < _population.size() / 2; i++) {
            if (JMetalRandom.getInstance().nextDouble() < 0.5) {
                int[] index = getIndexCrossover(pMask[i], invertArray(qMask[i]));
                if (index != null) {
                    if (score[index[0]] > score[index[1]]) oMask[i][index[0]] = false;
                    else oMask[i][index[1]] = false;
                }
            } else {
                int[] index = getIndexCrossover(invertArray(pMask[i]), qMask[i]);
                if (index != null) {
                    if (score[index[0]] < score[index[1]]) oMask[i][index[0]] = qMask[i][index[0]];
                    else oMask[i][index[1]] = qMask[i][index[1]];
                }
            }
        }

        // Mutation
        for (int i = 0; i < _population.size() / 2; i++) {
            //TODO
            if (countBool(oMask[i], true) == 0) oMask[i][JMetalRandom.getInstance().nextInt(0, oMask[i].length - 1)] = true;
            if (countBool(oMask[i], false) == 0) oMask[i][JMetalRandom.getInstance().nextInt(0, oMask[i].length - 1)] = false;
            //TODO

            if (JMetalRandom.getInstance().nextDouble() < 0.5) {
                int[] index = randomFromNonZero(oMask[i]);
                if (score[index[0]] > score[index[1]]) oMask[i][index[0]] = false;
                else oMask[i][index[1]] = false;
            } else {
                int[] index = randomFromNonZero(invertArray(oMask[i]));
                if (score[index[0]] < score[index[1]]) oMask[i][index[0]] = true;
                else oMask[i][index[1]] = true;
            }
        }

        List<BinaryCSOSolution> _population_variation = new ArrayList<>(_population.size());
        for (boolean[] b : oMask) {
            BinaryCSOSolution s = new BinaryCSOSolution(Collections.singletonList(this.numberOfBits), problem.numberOfObjectives());
            BitSet bits = s.variables().get(0);
            for (int i = 0; i < b.length; i++)
                bits.set(i, b[i]);

            ((StaticCSO) problem).intelligentSwitchOff(s);

            problem.evaluate((S) s);
//            problem.evaluateConstraints(s);
            _population_variation.add(s);

            evaluations++;
        }

        return _population_variation;
    }

    /**
     * Generate the reverse of a boolean array.
     *
     * @param array Boolean array
     * @return Reverse of the array
     */
    public boolean[] invertArray(boolean[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = !array[i];
        }

        return array;
    }

    /**
     * Return a random index of the array that contains 1.
     *
     * @param array Boolean array
     * @return
     */
    public int[] randomFromNonZero(boolean[] array) {
        List<Integer> nonZero = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            if (array[i]) nonZero.add(i);
        }

        int m = JMetalRandom.getInstance().nextInt(0, nonZero.size() - 1);
        int n = JMetalRandom.getInstance().nextInt(0, nonZero.size() - 1);

        return new int[]{m, n};
    }

    /**
     * Generate the intersection between two boolean arrays and returns index for crossover.
     *
     * @param array1 First boolean array
     * @param array2 Second boolean array
     * @return Index for crossover
     */
    public int[] getIndexCrossover(boolean[] array1, boolean[] array2) {
        List<Integer> inter = new ArrayList<>();
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] && array2[i]) inter.add(i);
        }
        int m, n;
        if (inter.size() > 0) {
            m = JMetalRandom.getInstance().nextInt(0, inter.size() - 1);
            n = JMetalRandom.getInstance().nextInt(0, inter.size() - 1);
            return new int[]{m, n};
        } else {
            return null;
        }

    }

    public int countBool(boolean[] array, boolean bool) {
        int counter = 0;
        for (boolean b : array) {
            if (b == bool) counter++;
        }
        return counter;
    }
}