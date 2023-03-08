package org.nextsus.cso.ela;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.ela.sampling.Walk;
import org.nextsus.cso.ela.sampling.impl.AdaptiveWalk;
import org.nextsus.cso.ela.sampling.impl.RandomWalk;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class SamplingMain {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        StaticCSO problem;
        Algorithm<List<BinaryCSOSolution>> algorithm;
        CrossoverOperator<BinaryCSOSolution> crossover;
        MutationOperator<BinaryCSOSolution> mutation;
        SelectionOperator<List<BinaryCSOSolution>, BinaryCSOSolution> selection;

        final int walkLength_maxEvaluations = Integer.parseInt(args[0]); // Stopping condition
        final int run = Integer.parseInt(args[1]);        // Seed selection
        final int taskID = Integer.parseInt(args[2]);     // Task ID (for filename)
        final int jobID = Integer.parseInt(args[3]);      // Job ID (for filename)
        final String main = "main.properties";            // Main configuration file
        final String scenario = args[4];                  // Scenario type
        final String alg = args[5];                       // Algorithm
        final String frontFile = args.length == 8 ? args[6] : null;   // Front file for initial solution
        final int numberOfThreads = args.length == 8 ? Integer.parseInt(args[7]) : 0;

        Neighborhood.NeighborhoodType neighborhoodType = Neighborhood.NeighborhoodType.Hamming;

        problem = new StaticCSO(main, scenario, run);

        if (frontFile == null) {

            Walk walk = switch (alg) {
                case "adaptive" -> new AdaptiveWalk(problem, walkLength_maxEvaluations, problem.getTotalNumberOfActivableCells(), neighborhoodType);
//            case "PLS" -> new PLS(problem, walkLength, mutationProbability, problem.getTotalNumberOfActivableCells());
                default -> new RandomWalk(problem, walkLength_maxEvaluations, problem.getTotalNumberOfActivableCells(), neighborhoodType);
            };

            List<BinaryCSOSolution> population = walk.execute();

            System.out.println("\n# Execution completed #\n");

            // Set the output directory according to the system (config folder if Condor or Windows, out folder if Picasso or UNIX system)
            String name = scenario + "." + alg + "." + run + "." + taskID + "." + jobID + ".csv";
            String FUN = "FUN." + name;
            String VAR = "VAR." + name;

            new SolutionListOutput(population).setVarFileOutputContext(new DefaultFileOutputContext(VAR, ",")).setFunFileOutputContext(new DefaultFileOutputContext(FUN, ",")).print();

            System.out.println("Objectives values have been written to file " + FUN);
            System.out.println("Variables values have been written to file " + VAR);
        } else {

            List<String> frontLines = new ArrayList<>();
            Scanner sc = new Scanner(new File(frontFile));
            while (sc.hasNext())
                frontLines.add(sc.next());
            sc.close();

            List<StaticCSO> staticCSOList = Collections.synchronizedList(new ArrayList<>());
            List<Boolean> problemAvailable = Collections.synchronizedList(new ArrayList<>());
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                staticCSOList.add(new StaticCSO(problem));
                problemAvailable.add(true);
            }


            for (int i = 0; i < frontLines.size(); i++) {
                final int idx = i;
                Runnable runnable = () -> {

                    StaticCSO staticCSO = null;
                    int staticCSOidx = -1;
                    while (staticCSO == null) {
                        for (int j = 0; j < numberOfThreads; j++) {
                            synchronized (problemAvailable) {
                                if (problemAvailable.get(j)) {
                                    staticCSOidx = j;
                                    staticCSO = staticCSOList.get(j);
                                    problemAvailable.set(j, false);
                                    break;
                                }
                            }
                        }
                    }

                    BinarySet binarySet = new BinarySet(staticCSO.getTotalNumberOfActivableCells());
                    for (int j = 0; j < binarySet.getBinarySetLength(); j++) {
                        binarySet.set(j, frontLines.get(idx).charAt(j) == '1');
                    }
                    BinaryCSOSolution solution = new BinaryCSOSolution(binarySet);

                    Walk walk = switch (alg) {
                        case "adaptive" -> new AdaptiveWalk(staticCSO, walkLength_maxEvaluations, staticCSO.getTotalNumberOfActivableCells(), neighborhoodType);
                        default -> new RandomWalk(staticCSO, walkLength_maxEvaluations, staticCSO.getTotalNumberOfActivableCells(), neighborhoodType);
                    };

                    walk.setInitialSolution(solution);

                    List<BinaryCSOSolution> population = walk.execute();
                    problemAvailable.set(staticCSOidx, true);

                    System.out.println("\n# Execution completed #\n");

                    // Set the output directory according to the system (config folder if Condor or Windows, out folder if Picasso or UNIX system)
                    String name = scenario + "." + alg + "." + run + "." + taskID + "." + jobID + "-" + idx + ".csv";
                    String FUN = "FUN." + name;
                    String VAR = "VAR." + name;

                    new SolutionListOutput(population).setVarFileOutputContext(new DefaultFileOutputContext(VAR, ",")).setFunFileOutputContext(new DefaultFileOutputContext(FUN, ",")).print();

                    System.out.println("Objectives values have been written to file " + FUN);
                    System.out.println("Variables values have been written to file " + VAR);
                };
                threads.add(new Thread(runnable));
            }

            List<Thread> threadsToJoin = new ArrayList<>();
            for (Thread thread : threads) {
                while (Thread.activeCount() - 2 >= numberOfThreads) {
                    for (Thread t : threadsToJoin)
                        t.join();
                    threadsToJoin.clear();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                thread.start();
                threadsToJoin.add(thread);
            }

            for (Thread t : threadsToJoin)
                t.join();
            threadsToJoin.clear();
        }
    }
}
