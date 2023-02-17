package org.nextsus.cso.util;

import org.uma.jmetal.solution.pointsolution.PointSolution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.legacy.front.Front;
import org.uma.jmetal.util.legacy.front.impl.ArrayFront;
import org.uma.jmetal.util.legacy.front.util.FrontUtils;

import java.io.File;
import java.io.FileNotFoundException;

public class ReferenceFrontFromDirectory {
    public static void main(String[] args) throws FileNotFoundException {
        File folder = new File(args[0]);
        NonDominatedSolutionListArchive<PointSolution> nonDominatedSolutionArchive = new NonDominatedSolutionListArchive<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.getName().endsWith(".csv")) {
                Front front = new ArrayFront(folder + "/" + fileEntry.getName(), " ");
                nonDominatedSolutionArchive.addAll(FrontUtils.convertFrontToSolutionList(front));
            }
        }

        new SolutionListOutput(nonDominatedSolutionArchive.solutions()).printObjectivesToFile(folder + "/RPF.csv", ",");
    }
}