package org.nextsus.cso.algorithm;

import java.util.List;

import org.nextsus.cso.algorithm.mocell.HybridMOCell;
import org.nextsus.cso.algorithm.moead.HybridMOEAD;
import org.nextsus.cso.algorithm.nsgaii.HybridNSGAII;
import org.nextsus.cso.algorithm.smsemoa.HybridSMSEMOA;
import org.nextsus.cso.algorithm.sparseea.HybridSparseEA;
import org.nextsus.cso.operator.crossover.*;
import org.nextsus.cso.operator.mutation.BSM;
import org.nextsus.cso.operator.mutation.GeographicMutation;
import org.nextsus.cso.operator.mutation.SectorM;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.legacy.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.util.neighborhood.impl.C9;

public class CSOMain {
    public static void main(String[] args) {
        StaticCSO problem;
        Algorithm<List<BinaryCSOSolution>> algorithm;
        CrossoverOperator<BinaryCSOSolution> crossover;
        MutationOperator<BinaryCSOSolution> mutation;
        SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selection;

        int popSize = Integer.parseInt(args[0]);    // Population size
        int numEvals = Integer.parseInt(args[1]);   // Stopping condition
        int run = Integer.parseInt(args[2]);        // Seed selection
        int taskID = Integer.parseInt(args[3]);     // Task ID (for filename)
        int jobID = Integer.parseInt(args[4]);      // Job ID (for filename)
        String main = "main.properties";            // Main configuration file
        String scenario = args[5];                  // Scenario type
        String alg = args[6];                       // Algorithm

        problem = new StaticCSO(main, scenario, run);

        double crossoverProbability = 0.9;
        double mutationProbability = 1.0 / problem.getTotalNumberOfActivableCells();

        // Crossover and mutation
//        if (alg.equals("moead")) {
//            crossover = new BinaryTwoPointCrossoverMOEAD<BinaryCSOSolution>(crossoverProbability);
//        } else {
//            crossover = new BinaryTwoPointCrossover<BinaryCSOSolution>(crossoverProbability);
//        }
//
//        mutation = new BitFlipMutation<>(mutationProbability);

//        crossover = new RGX<>(crossoverProbability, 50, problem.getUDN());
//        crossover = new SectorX<>(crossoverProbability, 3, problem.getUDN());
        crossover = new TowerX<>(crossoverProbability, 1, problem.getUDN());

//        mutation = new GeographicMutation<>(crossoverProbability, 50, problem.getUDN());
        mutation = new SectorM<>(crossoverProbability, 1, problem.getUDN());

        selection = new BinaryTournamentSelection<>();

        algorithm = switch (alg) {
            case "nsga2" -> new HybridNSGAII<>(problem, numEvals, popSize, popSize, popSize, crossover, mutation, selection, new DefaultDominanceComparator<>(), new SequentialSolutionListEvaluator<>());
            case "mocell" -> new HybridMOCell<>(problem, numEvals, popSize, new CrowdingDistanceArchive<>(popSize), new C9<>((int) Math.sqrt(popSize), (int) Math.sqrt(popSize)), crossover, mutation, selection, new SequentialSolutionListEvaluator<>());
            case "moead" -> new HybridMOEAD<>(problem, popSize, popSize, numEvals, mutation, crossover, AbstractMOEAD.FunctionType.TCHE, "", 0.1, 2, 20);
            case "smsemoa" -> new HybridSMSEMOA<>(problem, numEvals, popSize, 100.0, crossover, mutation, selection, new DefaultDominanceComparator<>(), new PISAHypervolume<>());
            case "sparseea" -> new HybridSparseEA<>(problem, numEvals, popSize, crossoverProbability, mutationProbability, new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>()), problem.getTotalNumberOfActivableCells());
            default -> new HybridNSGAII<>(problem, numEvals, popSize, popSize, popSize, crossover, mutation, selection, new DefaultDominanceComparator<>(), new SequentialSolutionListEvaluator<>());
        };

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

        System.out.println("\n# Execution completed #\n");

        List<BinaryCSOSolution> population = algorithm.getResult();

        // To remove infeasible solutions (both objectives are 0.0)
        population.removeIf(s -> s.objectives()[0] == 0.0 && s.objectives()[1] == 0.0);

        // To change the sign of the capacity
        population.forEach((s) -> {
            s.objectives()[1] = -s.objectives()[1];
        });

        // Set the output directory according to the system (config folder if Condor or Windows, out folder if Picasso or UNIX system)
        String name = alg + "_b_" + run;
        String FUN = name + ".FUN." + taskID + "." + jobID + ".csv";
        String VAR = name + ".VAR." + taskID + "." + jobID + ".csv";

        new SolutionListOutput(population).setVarFileOutputContext(new DefaultFileOutputContext(VAR, ",")).setFunFileOutputContext(new DefaultFileOutputContext(FUN, ",")).print();

        System.out.println("Total execution time: " + algorithmRunner.getComputingTime() + " ms (" + (int) (algorithmRunner.getComputingTime() / 1000) / 60 + ":" + (int) (algorithmRunner.getComputingTime() / 1000) % 60 + " minutes)");
        System.out.println("Objectives values have been written to file " + FUN);
        System.out.println("Variables values have been written to file " + VAR);

//        PlotFront plot = new Plot3D(new ArrayFront(population).getMatrix(), problem.getName() + " (NSGA-II)");
//        plot.plot();
//        PlotFront plot = new PlotSmile(new ArrayFront(population).getMatrix(), problem.getName() + " (NSGA-II)");
//        plot.plot();
    }
}
