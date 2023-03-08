package org.nextsus.cso.model;

import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.cells.Sector;

import java.io.Serializable;
import java.util.*;

/**
 * Class representing a point of the grid
 */
public class Point implements Serializable {
    UDN udn;

    // Point coordinates
    int x;
    int y;
    int z;

    Map<Double, BTS> installedBTS;

    Region propagationRegion;

    Map<Double, Double> totalReceivedPower;

    //Used to precompute stats only when needed_
    boolean statsComputed;

    //Function<Cell, Double> function_;

    Map<Integer, Double> signalPowerMap = new HashMap<>();
    Map<Integer, Double> snrMap = new HashMap<>();

    boolean canDeploy;

    /**
     * Constructor
     *
     * @param x Coordinate x
     * @param y Coordinate y
     * @param z Coordinate z
     */
    Point(UDN udn, int x, int y, int z) {
        this.udn = udn;
        this.x = x;
        this.y = y;
        this.z = z;

        installedBTS = new TreeMap<>();
        propagationRegion = null;
        totalReceivedPower = null;
        statsComputed = false;
    }

    public Region getPropagationRegion() {
        return this.propagationRegion;
    }

    void setPropagationRegion(Region r) {
        propagationRegion = r;
    }

    /**
     * Computes the signal power received at this grid point from the Cell c
     *
     * @param c The serving Cell
     * @return The received power
     */
    public double computeSignalPower(Cell c) {
        double power;

        if (signalPowerMap.get(c.getID()) == null) {
            Sector sec = c.getSector();
            double receptorGain = sec.getReceptorGain();
            double transmitterGain = sec.getTransmitterGain();
            double waveLength = c.getWavelength();
            double transmitterPower = 10 * Math.log10(sec.getTransmittedPower());
            int[] angles = udn.calculateAngles(this, sec.getBTS());
            double attenuationFactor = 10 * Math.log10(c.getAttenuationFactor(angles[0], angles[1]));

            List<Point> segments = new MultiSegment(this, c).divideSegment();   // Obtain a discrete line divided by segments between the Point and the Cell
            Point previous = segments.get(0);               // Cell, first point of the segment
            double loss = 0;
            for (int i = 1; i < segments.size(); i++) {
                Point p = segments.get(i);
                double distanceToPrevious = udn.distance(p.x, p.y, p.z, previous.x, previous.y, previous.z); // Distance from p to previous Point
                double pathLoss = p.getPropagationRegion().getPathloss();
                double totalDistance = udn.distance(p.x, p.y, p.z, sec.getX(), sec.getY(), sec.getZ());
                double previousDistanceToCell = udn.distance(previous.x, previous.y, previous.z, sec.getX(), sec.getY(), sec.getZ());
                loss += i == 1 ? 10 * pathLoss * Math.log10(waveLength / (4.0 * Math.PI * distanceToPrevious)) : 10 * pathLoss * Math.log10(previousDistanceToCell / totalDistance);
                previous = p;
            }

            power = receptorGain + transmitterGain + transmitterPower + attenuationFactor + loss;
            signalPowerMap.put(c.getID(), power);
        } else {
            power = signalPowerMap.get(c.getID());
        }

        return power;
    }

    /**
     * Returns the closest social attractor to this point
     *
     * @return Closest SA
     */
    SocialAttractor getClosestSA(UDN udn) {

        SocialAttractor sa = null;
        double minDistance = Double.MAX_VALUE;
        double d;

        for (SocialAttractor s : udn.getSocialAttractors()) {
            d = udn.distance(this.x, this.y, this.z, s.getX(), s.getY(), s.getZ());
            if (d < minDistance) {
                minDistance = d;
                sa = s;
            }
        }

        return sa;
    }

    /**
     * Distance to the BTS of the given cell.
     *
     * @param c Cell
     * @return The Euclidean distance from this point to the Cell BTS
     */
    private double distanceToBTS(Cell c) {
        return this.udn.distance(this.x, this.y, this.z, c.getBTS().getX(), c.getBTS().getY(), c.getBTS().getZ());
    }

    /**
     * Computes the received SNR from a given cell
     *
     * @param c Cell
     * @return SNR received by c
     */
    public double computeSNR(Cell c) {

        // Get the bandwidth of the Cell and its working frequency
        double cellBW = c.getTotalBW();
//        double frequency = c.getBTS().getWorkingFrequency();

        // Compute the noise
        double pn = -174 + 10.0 * Math.log10(cellBW * 1000000);

        // Get the averaged power received at the BTS working frequency
//        double totalPower = this.totalReceivedPower_.get(frequency);

        // Compute the power received at this point from Cell c
        double power = this.computeSignalPower(c);

        //double distance = this.udn_.distance(this.x_, this.y_, c.getBTS().getX(), c.getBTS().getY());

        return power - pn; //(totalPower - power + pn);
    }

    /**
     * Precomputes the averaged SNR at each grid point, for each
     */
    public void computeTotalReceivedPower() {
        //allocate memory at this point
        totalReceivedPower = new TreeMap<>();

        double sum, power;

        for (double frequency : this.udn.getCells().keySet()) {
            sum = 0.0;
            for (Cell c : this.udn.getCells().get(frequency)) {
                if (c.isActive()) {
                    power = computeSignalPower(c);
                    //dB -> mW
                    power = Math.pow(10.0, power / 10);
                    sum += power;
                }
            }

            this.totalReceivedPower.put(frequency, sum);
        }
    }

    /**
     * Return the closest BTS in terms of the received signal power. Required by
     * M. Mirahsan, R. Schoenen, and H. Yanikomeroglu, “HetHetNets:
     * Heterogeneous Traffic Distribution in Heterogeneous Wireless Cellular
     * Networks,” IEEE J. Sel. Areas Commun., vol. 33, no. 10, pp. 2252–2265,
     * 2015.
     *
     * @return The closest BTS
     */
    Cell getCellWithHigherReceivingPower() {
        double power;
        double maxPower = Double.NEGATIVE_INFINITY;
        Cell closest = null;

        for (Double frequency : udn.getCells().keySet()) {
            for (Cell c : udn.getCells().get(frequency)) {
                power = computeSignalPower(c);
                if (power > maxPower) {
                    maxPower = power;
                    closest = c;
                }
            }
        }

        return closest;
    }

    /**
     * Returns the cell that serves with the best SNR, regardless of its
     * operating frequency.
     *
     * @return The cell with higher SNR
     */
    public Cell getCellWithHigherSNR() {
        double sinr;
        Map<Double, Double> maxSNR = new TreeMap<>();
        Map<Double, Cell> servingBTSs = new TreeMap<>();
        Cell servingCell = null;

        for (Double frequency : udn.getCells().keySet()) {
            maxSNR.put(frequency, Double.NEGATIVE_INFINITY);
        }

        for (Double frequency : udn.getCells().keySet()) {
            for (Cell c : udn.getCells().get(frequency)) {
                if (c.isActive()) {
                    sinr = computeSNR(c);

                    //quality, regardless of the cell activation
                    if (sinr > maxSNR.get(frequency)) {
                        maxSNR.put(frequency, sinr);
                        servingBTSs.put(frequency, c);
                    }
                }
            }
        }

        // If using cellsOfInterestByPoint in UDN
//        for (double frequency : this.udn_.cellsOfInterestByPoint.get(this).keySet()) {
//            for (Cell c : this.udn_.cellsOfInterestByPoint.get(this).get(frequency)) {
//                if (c.isActive()) {
//                    sinr = this.computeSINR(c);
//                    if (sinr > maxSNR.get(frequency)) {
//                        maxSNR.put(frequency, sinr);
//                        servingBTSs.put(frequency, c);
//                    }
//                }
//            }
//        }

        //retrieve the best among the precomputed values
        double maxValue = Double.NEGATIVE_INFINITY;
        for (Double f : servingBTSs.keySet()) {
            sinr = maxSNR.get(f);
            if (sinr > maxValue) {
                maxValue = sinr;
                servingCell = servingBTSs.get(f);
            }
        }

        return servingCell;
    }

    public Cell getCellWithHigherSNRByType(UDN.CellType type) {
        double sinr;
        double maxSNR = Double.NEGATIVE_INFINITY;
        Cell servingCell = null;

        for (Double frequency : udn.getCells().keySet()) {
            if (!udn.getCells().get(frequency).isEmpty()) {
                if (udn.getCells().get(frequency).get(0).getType().equals(type)) {
                    for (Cell c : udn.getCells().get(frequency)) {
                        if (c.isActive()) {
                            sinr = this.computeSNR(c);

                            if (sinr > maxSNR) {
                                maxSNR = sinr;
                                servingCell = c;
                            }
                        }
                    }
                }
            }
        }

        return servingCell;
    }

    /**
     * Return a sorted list of Cells with the best serving SINR, regardless of
     * their operation frequency and type
     *
     * @return Sorted cells
     */
    public SortedMap<Double, Cell> getCellsWithBestSNRs() {
        // Create the comparator for the sortedlist
        Comparator<Double> cellSINRComparator = (snr1, snr2) -> Double.compare(snr2, snr1);
        SortedMap<Double, Cell> sortedCells = new TreeMap<>(cellSINRComparator);

        for (Double frequency : udn.getCells().keySet()) {
            for (Cell c : udn.getCells().get(frequency)) {
                sortedCells.put(computeSNR(c), c);
            }
        }

        return sortedCells;
    }

    Map<Double, Double> getTotalReceivedPower() {
        return this.totalReceivedPower;
    }

    public boolean hasBTSInstalled() {
        return !installedBTS.isEmpty();
    }

    public boolean hasBTSInstalled(double workingFrequency) {
        return installedBTS.containsKey(workingFrequency);
    }

    public void addInstalledBTS(double workingFrequency, BTS bts) {
        if (!installedBTS.containsKey(workingFrequency)) {
            installedBTS.put(workingFrequency, bts);
        }
    }

    public Map<Double, BTS> getInstalledBTS() {
        return this.installedBTS;
    }

    public List<Cell> getCells() {
        List<Cell> cells = new ArrayList<>();

        installedBTS.keySet().forEach(d -> installedBTS.get(d).getSectors().forEach(sector -> cells.addAll(sector.getCells())));

        return cells;
    }

    public List<Cell> getActiveCells() {
        List<Cell> activeCells = new ArrayList<>();

        for (double d : installedBTS.keySet()) {
            for (Sector sector : installedBTS.get(d).getSectors()) {
                for (Cell cell : sector.getCells()) {
                    if (cell.isActive()) activeCells.add(cell);
                }
            }
        }

        return activeCells;
    }

    public int[] getPoint2D() {
        return new int[]{this.x, this.y};
    }
}