package org.nextsus.cso.problem;

import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.Random;

/**
 * @author paco
 */
public class Main {

    public static void main(String[] args) throws ClassNotFoundException {
        //UDN udn = new UDN("main.conf", "cells.conf", "users.conf");
        /*PlanningUDN p = new PlanningUDN("main.conf", "cells.conf", "socialAttractors.conf", "users.conf");

        //udn.printPropagationRegion();

        //udn.printVoronoi();
        Solution planning = new Solution(p);
        p.setBasicPlanning(planning);
        p.evaluate(planning);
        p.setHigherCapacityPlanning(planning);
        p.evaluate(planning);
//        Simulation sim = new Simulation(udn,60,1.0);
        //udn.printUsers(); */
        int simTime = 0;
        String scenario = "LL";
        StaticCSO cso = new StaticCSO("main.conf", scenario, 0);
        Random r = new Random();

        // Specific solution
        String solutionString = new String(
                "001000000001000000000000100100010000000000000000000000000000000000000000000000000000010001000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010001000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000100000000000100000000000000000000000000000000000000000000100000000000000000000000000000000010000000000000000001100000100000000000000000000000000000000000000000101010000000000101000000000000000100000000000000010001100000000000");
        System.out.println(solutionString.length());

        boolean[] solutionBool = new boolean[solutionString.length()];

        for (int i = 0; i < solutionString.length(); i++) {
            solutionBool[i] = solutionString.charAt(i) != '0';
        }

        BinaryCSOSolution s = new BinaryCSOSolution(cso.getListOfBitsPerVariable(),
                cso.numberOfObjectives());
        BinarySet bits = s.variables().get(0);
        for (int i = 0; i < bits.getBinarySetLength(); i++) {
            bits.set(i, solutionBool[i]);
        }

        // Random solution
//        BinarySolution s = new DefaultBinarySolution(cso.getListOfBitsPerVariable(), cso.getNumberOfObjectives());
//        BitSet bits = s.variables().get(0);
//        for (int i = 0; i < bits.length(); i++) {
//            bits.set(i, r.nextBoolean());
//        }

        cso.evaluate(s);
        System.out.println(s.objectives()[0] + " " + s.objectives()[1]);
//        System.out.println("Number of active cells: " + cso.udn_.getTotalNumberOfActiveCells());
//        cso.intelligentSwitchOff(s);
//        cso.evaluate(s);
//        System.out.println("Number of active cells: " + cso.udn_.getTotalNumberOfActiveCells());
//        System.out.println(s.getDecisionVariables()[0]);
//        cso.udn_.printGridNew();   // Comment for no debug info
        //cso.evaluate(s);
        //cso.udn_.printGridNew();

//        System.out.println("s = " + s);
//        System.out.println("visited points = " + cso.pointsWithStatsComputed());
    }
}
