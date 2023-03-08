package org.nextsus.cso.ela;

import org.nextsus.cso.ela.features.LocalFeatures;
import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FeaturesMain {
    public static void main(String[] args) throws InterruptedException {
        String path = args[0];
        String[] pathSplitted = path.split("/");
        String fileName = pathSplitted[pathSplitted.length - 1];
        String[] params = fileName.split("\\.");

        double percentage = Double.parseDouble(args[1]);

        int numberOfThreads = (args.length == 3) ? Integer.parseInt(args[2]) : Runtime.getRuntime().availableProcessors();

        System.out.println(numberOfThreads);

        // scenario = params[1];
        // alg = params[2];
        // run = params[3];

        Scanner sc = null;
        try {
            sc = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            System.exit(-1);
        }

        StaticCSO problem = new StaticCSO("main.properties", params[1], Integer.parseInt(params[3]));

        List<BinaryCSOSolution> solutionSet = new ArrayList<>();
        while (sc.hasNext()) {
            String tmp = sc.next();
            BinarySet binarySet = new BinarySet(problem.getTotalNumberOfActivableCells());
            for (int i = 0; i < binarySet.getBinarySetLength(); i++)
                binarySet.set(i, tmp.charAt(i) == '1');
            solutionSet.add(new BinaryCSOSolution(binarySet));
        }
        sc.close();

        new LocalFeatures(problem, solutionSet, params[2], Neighborhood.NeighborhoodType.Hamming, percentage, numberOfThreads).execute();
    }
}
