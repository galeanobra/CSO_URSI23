package org.nextsus.cso.ela;

import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.util.List;

public class ELAMain {
    public static void main(String[] args) {
        StaticCSO problem;
        Algorithm<List<BinaryCSOSolution>> algorithm;
        CrossoverOperator<BinaryCSOSolution> crossover;
        MutationOperator<BinaryCSOSolution> mutation;
        SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selection;


        int maxEvals = Integer.parseInt(args[0]);   // Stopping condition
        int run = Integer.parseInt(args[1]);        // Seed selection
        int taskID = Integer.parseInt(args[2]);     // Task ID (for filename)
        int jobID = Integer.parseInt(args[3]);      // Job ID (for filename)
        String main = "main.properties";            // Main configuration file
        String scenario = args[4];                  // Scenario type
        String alg = args[5];                       // Algorithm

        problem = new StaticCSO(main, scenario, run);

        double mutationProbability = 10.0 / problem.getTotalNumberOfActivableCells();

        PLS pls = new PLS(problem, maxEvals, mutationProbability, problem.getTotalNumberOfActivableCells());

        List<BinaryCSOSolution> population = pls.execute();

        System.out.println("\n# Execution completed #\n");

        // Set the output directory according to the system (config folder if Condor or Windows, out folder if Picasso or UNIX system)
        String name = alg + "_b_" + run;
        String FUN = name + ".FUN." + taskID + "." + jobID + ".csv";
        String VAR = name + ".VAR." + taskID + "." + jobID + ".csv";

        new SolutionListOutput(population).setVarFileOutputContext(new DefaultFileOutputContext(VAR, ",")).setFunFileOutputContext(new DefaultFileOutputContext(FUN, ",")).print();

        System.out.println("Objectives values have been written to file " + FUN);
        System.out.println("Variables values have been written to file " + VAR);
    }
}