package org.nextsus.cso.problem;

import org.nextsus.cso.model.StaticUDN;
import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.cells.Sector;
import org.nextsus.cso.model.users.User;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.util.Arrays;
import java.util.List;

/**
 * Class representing the Static Cell-Switch Off problem
 */
public class StaticCSO extends CSO {

    /**
     * Creates an instance of the Static CSO problem
     */
    public StaticCSO(String mainConfig, String scenario, int run) {

        udn_ = new StaticUDN(mainConfig, scenario, run);

        bits = udn_.getTotalNumberOfActivableCells();

        udn_.getTotalNumberOfActivableCells();

        run_ = run;

        loadOperatorsConfig(udn_.getOperatorsFile());

        System.out.println("\n# Execution started #\n");

//        udn_.getCellsOfInterestByPoint();
    }

    @Override
    public int numberOfVariables() {
        return 1;
    }

    @Override
    public int numberOfObjectives() {
        return 2;
    }

    @Override
    public int numberOfConstraints() {
        return 0;
    }

    @Override
    public String name() {
        return "StaticCSO";
    }

    /**
     * Evaluates a solution.
     *
     * @param solution The solution to evaluate.
     */
    @Override
    public BinaryCSOSolution evaluate(BinaryCSOSolution solution) {
        BinarySet cso = solution.variables().get(0);

        // Check if there are active cells
        boolean noActiveCells = cso.cardinality() == 0;

        if (!noActiveCells) {
            // Map the activation to the UDN
            udn_.setCellActivation(cso);

            // Recompute the total received power for each user point
//            udn_.computeSignaling();

            double capacity = networkCapacity(solution);
            double powerConsumption = powerConsumptionStatic();

            solution.objectives()[0] = powerConsumption;
            solution.objectives()[1] = -capacity;
        } else {
            solution.objectives()[0] = Double.MAX_VALUE;    // TODO
            solution.objectives()[1] = 0.0;
        }

        return solution;
    } // evaluate

    public double getCapacityUpperLimit() {
        return networkCapacityUpperLimit();
    }

    public double getConsumptionUpperLimit() {
        return powerConsumptionUpperLimit();
    }

    /**
     * m
     * In this function operators are applied in order to improve the sinr of
     * certain problematic points in the network by switching off some BTS
     *
     * @param solution: Solution to be modified
     */
    public void intelligentSwitchOff(BinaryCSOSolution solution) {
        if (this.operators_.containsKey("singleCellOperator")) singleCellOperator(this.operators_.get("singleCellOperator"), solution);
        if (this.operators_.containsKey("emptyCellOperator")) emptyCellOperator(this.operators_.get("emptyCellOperator"), solution);
        if (this.operators_.containsKey("prioritizeSmallCellsOperator")) prioritizeSmallCellsOperator(this.operators_.get("prioritizeSmallCellsOperator"), solution);
        if (this.operators_.containsKey("prioritizeFemtoOperator")) prioritizeFemtoOperator(this.operators_.get("prioritizeFemtoOperator"), solution);
        if (this.operators_.containsKey("higherFrequencyOperator")) higherFrequencyOperator(this.operators_.get("higherFrequencyOperator"), solution);
    }

    /**
     * Calculates the power consumption taking into account the total traffic demand
     * and the maintenance power, in the case of small cells (pico, femto)
     *
     * @return Power consumption
     */
    double powerConsumptionStatic() {
        double sum = 0.0;
        boolean hasActiveCells;
        double maintenancePower = 2000; // mW

        for (List<BTS> btss : udn_.btss.values()) {
            for (BTS bts : btss) {
                hasActiveCells = false;
                for (Sector sector : bts.getSectors()) {
                    for (Cell cell : sector.getCells()) {
                        if (cell.isActive()) {
                            hasActiveCells = true;
                            sum += sector.getTransmittedPower() * sector.getAlfa() + sector.getBeta() + sector.getDelta() * cell.getTrafficDemand() + 10;
                        } else {
                            // Residual consumption in sleep mode (mW)
                            sum += sector.getTransmittedPower() * 0.01;
                        }
                    }
                }
                if (hasActiveCells) {
                    sum += maintenancePower;
                }
            }
        }

        //mW -> W -> kW -> MW
        sum /= 1000000;
        //System.out.println("Power consumption = " + sum);

        return sum;
    }// powerConsumptionStatic

    double powerConsumptionUpperLimit() {

        BinarySet binarySet = new BinarySet(getTotalNumberOfActivableCells());
        for (int i = 0; i < binarySet.getBinarySetLength(); i++)
            binarySet.set(i, true);

        udn_.setCellActivation(binarySet);
        udn_.resetNumberOfUsersAssignedToCells();

        for (User u : this.udn_.getUsers()) {
            u.setServingCell(udn_.getGridPoint(u.getX(), u.getY(), u.getZ()).getCellWithHigherSNR());
            u.getServingCell().setNumbersOfUsersAssigned(1);
        }

        return powerConsumptionStatic();
    }

    public UDN getUDN() {
        return udn_;
    }

    @Override
    public List<Integer> getListOfBitsPerVariable() {
        return Arrays.asList(bits);
    }
}
