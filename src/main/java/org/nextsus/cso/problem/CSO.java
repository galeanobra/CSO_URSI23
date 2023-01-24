package org.nextsus.cso.problem;

import org.nextsus.cso.model.Point;
import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.UDN.CellType;
import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.cells.Sector;
import org.nextsus.cso.model.users.User;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.pseudorandom.impl.JavaRandomGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.nextsus.cso.model.UDN.CellType.*;

/**
 * Class representing the Cell-Switch Off problem
 */
public abstract class CSO extends AbstractBinaryCSOProblem {

    /**
     * Number of activable cells
     */
    protected int bits;

    /**
     * The underlying UDN
     */
    protected UDN udn_;

    /**
     * Operators configuration (operators names and application rates)
     */
    protected Map<String, Double> operators_;

    /**
     * The seed selection to generate the instance
     */
    protected int run_;

    public int getTotalNumberOfActivableCells() {
        return udn_.getTotalNumberOfActivableCells();
    }

    int pointsWithStatsComputed() {
        return udn_.pointsWithStatsComputed();
    }

    double powerConsumptionBasedOnTransmittedPower() {
        double sum = 0.0;

        for (List<Cell> cells : udn_.cells.values()) {
            for (Cell c : cells) {
                if (c.isActive()) {
                    sum += 4.7 * c.getSector().getTransmittedPower();
                } else {
                    //residual consuption in sleep mode (mW)
                    sum += 160;
                }
            }
        }
        //mW -> W -> kW -> MW
        sum /= 1000000000;

        return sum;
    }

    /**
     * Calculates the power consumption taking into account the total traffic demand
     *
     * @return Power consumption
     */
    double powerConsumptionPiovesan() {
        double sum = 0.0;

        for (List<Cell> cells : udn_.cells.values()) {
            for (Cell c : cells) {
                Sector sector = c.getSector();
                if (c.isActive()) {
                    //sum += c.getBTS().getBaseConsumedPower() * 4.7 * c.getBTS().getTransmittedPower();
//                    double td = c.getTrafficDemand();
                    sum += sector.getTransmittedPower() * sector.getAlfa() + sector.getBeta() + sector.getDelta() * c.getTrafficDemand() + 10;
                } else {
                    //residual consuption in sleep mode (mW)
                    sum += sector.getTransmittedPower() * 0.01;
                }
            }
        }
        //mW -> W -> kW -> MW
        sum /= 1000000000;
        //System.out.println("Consumed power = " + sum);
        return sum;
    }

    void saveCellStatus(BinaryCSOSolution s) {
        BinarySet cso = s.variables().get(0);

        //map the activation to the udn
        udn_.copyCellActivation(cso);
    }

    /**
     * Max capacity of the 5G network. At each point, it returns the best SINR
     * for each of the different operating frequencies.
     *
     * @return Network capacity
     */
    double networkCapacity(BinaryCSOSolution solution) {

        /*
          For the dynamic problem addressing
         */
        List<Cell> assignment = new ArrayList<>();

        double capacity = 0.0;

        // 1. Reset number of users assigned to cells
        udn_.resetNumberOfUsersAssignedToCells();

        // 2. Assign users to cells, to compute the BW allocated to them
        for (User u : this.udn_.getUsers()) {
            u.setServingCell(udn_.getGridPoint(u.getX(), u.getY(), u.getZ()).getCellWithHigherSNR());
            u.getServingCell().addUserAssigned();

            // dynamic
            assignment.add(u.getServingCell());
        }

        // 3. Save the assignment into the solution
        solution.setUEsToCellAssignment(assignment);

        // 4. Computes the Mbps allocated to each user
        for (User u : this.udn_.getUsers()) {
            double allocatedBW = u.getServingCell().getSharedBWForAssignedUsers();
            double c = u.capacityMIMO(this.udn_, allocatedBW);
            capacity += c / 1000.0;
        }

        //udn_.validateUserAssigned();  // TODO esto qué es
        return capacity;
    }

    public int getRun() {
        return this.run_;
    }

    private double numberOfActiveCells() {
        int count = 0;

        for (List<Cell> cells : udn_.cells.values()) {
            for (Cell c : cells) {
                if (c.isActive()) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Sort a given list of Points by it SINR, being the worse the first
     *
     * @param l : list to sort
     * @return sorted list
     */
    public List<Point> sortList(List<Point> l) {
        double[] sinr_list = new double[l.size()];
        List<Point> sortedList = new ArrayList<>();
        double min_sinr = 5;

        for (int i = 0; i < l.size(); i++) {
            Point p = l.get(i);
            Cell c = p.getCellWithHigherSNR();
            double sinr = p.computeSNR(c);
            sinr_list[i] = sinr;

        }
        Arrays.sort(sinr_list);
        int index = 0;
        for (int i = 0; i < l.size(); i++) {
            for (int j = 0; j < l.size(); j++) {
                Point p_ = l.get(j);
                Cell c_ = p_.getCellWithHigherSNR();
                double sinr_ = p_.computeSNR(c_);
                if (Double.compare(sinr_, sinr_list[i]) == 0) {
                    index = j;
                    break;
                }
            }
            sortedList.add(i, l.get(index));
        }
        return sortedList;
    }

    /**
     * Cells with no users assigned are switched off.
     *
     * @param rate     : Application rate
     * @param solution The solution to be modified.
     */
    public void emptyCellOperator(double rate, BinaryCSOSolution solution) {
        if (new JavaRandomGenerator().nextDouble() < rate) {
            BinarySet cso = solution.variables().get(0);

            udn_.setCellActivation(cso);
            udn_.computeSignaling();
            udn_.resetNumberOfUsersAssignedToCells();

            if (udn_.getTotalNumberOfActiveCells() > 0) {

                // Assign users to cells, to compute the BW allocated to them
                for (User u : this.udn_.getUsers()) {
                    Point p = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                    Cell c = p.getCellWithHigherSNR();
                    c.addUserAssigned();
                    u.setServingCell(c);
                }

                for (double frequency : this.udn_.cells.keySet()) {
                    for (Cell c : udn_.cells.get(frequency)) {
                        if (c.getAssignedUsers() == 0) c.setActivation(false);
                    }
                }

                modifySolution(solution);
            }
            // System.out.println("The emptyCellOperator has turned off "+count+" cells");
        }//if
    }

    public void higherFrequencyOperator(double rate, BinaryCSOSolution solution) {
        if (new JavaRandomGenerator().nextDouble() < rate) {
            BinarySet cso = solution.variables().get(0);

            udn_.setCellActivation(cso);
            udn_.computeSignaling();
            udn_.resetNumberOfUsersAssignedToCells();

            if (udn_.getTotalNumberOfActiveCells() > 0) {

                // Assign users to cells
                for (User u : this.udn_.getUsers()) {
                    Point p = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                    Cell c = p.getCellWithHigherSNR();
                    c.addUserAssigned();
                    u.setServingCell(c);
                }

                for (User u : udn_.getUsers()) {
                    BTS b = u.getServingCell().getBTS();
                    Cell best = u.getServingCell();
                    Point p = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                    double max_sinr = p.computeSNR(u.getServingCell());

                    for (Sector s : b.getSectors()) {
                        for (Cell c : s.getCells()) {
                            if (!c.equals(u.getServingCell())) {
                                if (c.getType() == FEMTO) {
                                    double sinr = p.computeSNR(c);
                                    if (sinr >= max_sinr) {
                                        best = c;
                                        max_sinr = sinr;
                                    }
                                } else if (c.getType() == PICO) {
                                    if (u.getServingCell().getType() == PICO || u.getServingCell().getType() == MICRO || u.getServingCell().getType() == MACRO) {
                                        double sinr = p.computeSNR(c);
                                        if (sinr >= max_sinr) {
                                            best = c;
                                            max_sinr = sinr;
                                        }
                                    }
                                } else if (c.getType() == MICRO) {
                                    if (u.getServingCell().getType() == MICRO || u.getServingCell().getType() == MACRO) {
                                        double sinr = p.computeSNR(c);
                                        if (sinr >= max_sinr) {
                                            best = c;
                                            max_sinr = sinr;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    best.setActivation(true);

                    if (u.getServingCell().getAssignedUsers() == 1 && !u.getServingCell().equals(best)) {
                        u.getServingCell().setActivation(false);
                    }
                }
            }

            modifySolution(solution);
        }
    }


    /**
     * Switch on those femtocells that can serve UEs.
     *
     * @param solution Solution
     */
    public void prioritizeFemtoOperator(double rate, BinaryCSOSolution solution) {
        if (new JavaRandomGenerator().nextDouble() < rate) {
            BinarySet cso = solution.variables().get(0);

            udn_.setCellActivation(cso);
            udn_.computeSignaling();
            udn_.resetNumberOfUsersAssignedToCells();

            if (udn_.getTotalNumberOfActiveCells() > 0) {

                // Assign users to cells, to compute the BW allocated to them
                for (User u : this.udn_.getUsers()) {
                    Point p = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                    Cell c = p.getCellWithHigherSNR();
                    c.addUserAssigned();
                    u.setServingCell(c);
                }

                // Look for the candidate femtocells
                double threshold = 1;
                Cell alternative;
                Cell current;
                Point user_location;
                Map<Double, Cell> bestCells;

                for (User u : this.udn_.getUsers()) {
                    if ((u.getServingCell().getType() != FEMTO) || (u.getServingCell().getType() != PICO)) {
                        current = u.getServingCell();
                        user_location = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                        bestCells = user_location.getCellsWithBestSNRs();
                        for (Map.Entry<Double, Cell> actualEntry : bestCells.entrySet()) {
                            alternative = actualEntry.getValue();
                            if (user_location.computeSNR(alternative) > threshold) {
                                if ((alternative.getType() == FEMTO) || (alternative.getType() == PICO)) {
                                    u.setServingCell(alternative);
                                    alternative.addUserAssigned();
                                    current.removeUserAssigned();
                                    if (current.getAssignedUsers() == 0) current.setActivation(false);
                                    alternative.setActivation(true);
                                    break;
                                }
                            }
                        }
                    }//IF
                }//FOR

                emptyCellOperator(1, solution);

                modifySolution(solution);
            }
        }
    }


    /**
     * Switch on those small cells (pico and femto) that can serve UEs.
     *
     * @param solution Solution
     */
    public void prioritizeSmallCellsOperator(double rate, BinaryCSOSolution solution) {
        if (new JavaRandomGenerator().nextDouble() < rate) {
            BinarySet cso = solution.variables().get(0);

            udn_.setCellActivation(cso);
            udn_.computeSignaling();
            udn_.resetNumberOfUsersAssignedToCells();

            if (udn_.getTotalNumberOfActiveCells() > 0) {

                // Assign users to cells, to compute the BW allocated to them
                for (User u : this.udn_.getUsers()) {
                    Point p = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                    Cell c = p.getCellWithHigherSNR();
                    c.addUserAssigned();
                    u.setServingCell(c);
                }

                // Look for the candidate femtocells
                double threshold = 1;
                Cell alternative;
                Cell current;
                Point user_location;
                Map<Double, Cell> bestCells;

                for (User u : this.udn_.getUsers()) {
                    if ((u.getServingCell().getType() != FEMTO) || (u.getServingCell().getType() != PICO)) {
                        current = u.getServingCell();
                        user_location = udn_.getGridPoint(u.getX(), u.getY(), u.getZ());
                        bestCells = user_location.getCellsWithBestSNRs();
                        for (Map.Entry<Double, Cell> actualEntry : bestCells.entrySet()) {
                            alternative = actualEntry.getValue();
                            if (user_location.computeSNR(alternative) > threshold) {
                                if ((alternative.getType() == FEMTO) || (alternative.getType() == PICO)) {
                                    u.setServingCell(alternative);
                                    alternative.addUserAssigned();
                                    current.removeUserAssigned();
                                    if (current.getAssignedUsers() == 0) current.setActivation(false);
                                    alternative.setActivation(true);
                                    break;
                                }
                            }
                        }
                    }//IF
                }//FOR

                emptyCellOperator(1, solution);

                modifySolution(solution);
            }
        }//if
    }


    /**
     * Turn off those BTSs that only have one active cell, saving the maintenance power
     *
     * @param rate     : application probability
     * @param solution : Solution to be modified by the operator
     */
    public void singleCellOperator(double rate, BinaryCSOSolution solution) {
        if (new JavaRandomGenerator().nextDouble() < rate) {
            BinarySet cso = solution.variables().get(0);

            udn_.setCellActivation(cso);
            udn_.computeSignaling();
            udn_.resetNumberOfUsersAssignedToCells();

            if (udn_.getTotalNumberOfActiveCells() > 0) {
                // v1
                for (List<BTS> btss : udn_.btss.values()) {
                    for (BTS bts : btss) {
                        if (bts.getNumberOfActiveCells() == 1) {
                            //Turn off the active cell
                            for (Sector sector : bts.getSectors()) {
                                for (Cell cell : sector.getCells()) {
                                    if (cell.isActive()) {
                                        cell.setActivation(false);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                modifySolution(solution);
            }
        }//if
    }

    /**
     * Activates/deactivates BTSs in the solution according to the information
     * enclosed in the modified network of the problem
     *
     * @param solution Solution
     */
    public void modifySolution(BinaryCSOSolution solution) {
        BinarySet cso = solution.variables().get(0);
        int bts = 0;

        for (List<Cell> cells : udn_.cells.values()) {
            for (Cell c : cells) {
                if (c.getType() != CellType.MACRO) {
                    cso.set(bts, c.isActive());
                    bts++;
                }
            }
        }
    }

    /**
     * Extract operators configuration from file
     *
     * @param propFile Config file
     */
    public void loadOperatorsConfig(String propFile) {

        System.out.println("Loading operators config file...");

        Properties pro = new Properties();
        try (InputStream resourceStream = getClass().getResourceAsStream("/common/" + propFile)) {
            pro.load(resourceStream);
        } catch (IOException e) {
            System.out.println("Error loading properties " + propFile);
            System.exit(-1);
        }

        this.operators_ = new HashMap<>();
        int numOperators = Integer.parseInt(pro.getProperty("numOperators", "0"));

        for (int i = 1; i <= numOperators; i++) {
            String operatorName = pro.getProperty("operator" + i, "unknownOperator");
            double rate = Double.parseDouble(pro.getProperty("rate" + i, "0.0"));
            this.operators_.put(operatorName, rate);
        }


        if (!this.operators_.isEmpty()) {
            for (String operator : this.operators_.keySet())
                System.out.println("\t" + operator + ", rate: " + this.operators_.get(operator).toString());
        }
    }
} // CSO
