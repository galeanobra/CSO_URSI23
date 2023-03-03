/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nextsus.cso.model;

import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.cells.Sector;
import org.nextsus.cso.model.users.User;
import org.nextsus.cso.util.PPP;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.acos;

/**
 * @author paco
 */
public abstract class UDN {

    /**
     * Misc
     */
    public static Random random;
    static long seed;

    //terrain parameters
    public int gridPointsX;
    public int gridPointsY;
    public int gridPointsZ;
    public Map<Double, List<Cell>> cells;
    public Map<Double, List<BTS>> btss;
    public List<BTS> btsList;
    public List<Sector> sectorList;
    public Map<String, List<BTS>> towerList;
    public List<int[]> cellOrder;

    public Map<Point, Map<Double, List<Cell>>> cellsOfInterestByPoint;
    public double signalPowerThreshold = -90; // 0.000000000001;    // 1 pW

    //Users
    int usersTypes;
    List<String> usersConfig;
    int interPointSeparation;
    int terrainWidth;
    int terrainHeight;

    Point[][][] grid;

    boolean[][] pointsMap;

    /**
     * Propagation parameters
     */
    int numPropagationRegions;
    List<Region> propRegions;

    /**
     * Social Attractors
     */
    List<SocialAttractor> socialAttractors;
    double alphaHetHetNet;
    double meanBetaHetHetNet;
    /**
     * Users
     */
    List<User> users;
    long[] seeds_array = {6576946, 15806277, 10306509, 561099, 16435990, 1904859, 4080598, 16030968, 24070723, 5330056, 6787642, 11449375, 1801157, 11115944, 10621377, 14089326, 23581748, 12652207, 22237788, 1781623, 18336580, 22180257, 14134887, 9970106, 5431969, 24679879, 544331, 13649575, 15359920, 7049695, 15520211, 13756445, 7986543, 21061943, 10101328, 19447250, 5018584, 25009560, 16699832, 10563679, 7488476, 2692010, 17385187, 2484487, 12358331, 10457156, 22266884, 18504290, 5847318, 23121394, 54651432, 77546760, 97451320, 56328192, 39697568, 52408352, 84276000, 77263600, 46891340, 49417580, 93248320, 50346648, 57642204, 68574440, 54368272, 96986784, 39131252, 39878944, 37635864, 44365104, 75586840, 84740536, 94460544, 65300512, 82780616, 81749760, 33432864, 38383556, 42122024, 30906626, 64552820, 85023696, 75020520, 53620580, 99229864, 32402012, 60066660, 98482168, 34645092, 53337420, 48669888, 95491400, 43617408, 77829920, 78011296, 44648260, 56146820, 73060600, 40909796, 67362216};
    //Filenames of the configuration files
    String cellConfigFile;
    String hetNetConfigFile;
    String staticUserConfigFile;
    String mobilityType;
    String dynamicUserConfigFile;
    String mobilityConfigFile;
    String mobilityMatrixFile;
    String operatorsFile;
    String binaryMatrixFile;
    double lastfreq;

    /**
     * Default constructor
     *
     * @param mainConfigFile Filename of the main configuration file
     * @param run            The run to generate the same problem instances
     */
    public UDN(String mainConfigFile, String scenario, int run) {
        // Load cell configurations
        this.loadMainParameters(mainConfigFile);

        // Random seed if run == -1
        if (run == -1) {
            random = new Random();
        } else {
            random = new Random(this.seeds_array[run]);
        }

//        // TFM
//
//        try {
//            Scanner input = new Scanner(new File(this.binaryMatrixFile));
//            pointsMap = new boolean[gridPointsX_][gridPointsY_];
//
//            int x = 0;
//
//            while (input.hasNextLine()) {
//                int[] line = Arrays.stream(input.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
//                for (int y = 0; y < gridPointsY_; y++) {
//                    pointsMap[x][y] = line[y] == 1;
//                }
//                x++;
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        // Generate the grid
        grid = new Point[gridPointsX][gridPointsY][gridPointsZ];
        for (int i = 0; i < gridPointsX; i++) {
            grid[i] = new Point[gridPointsY][];
            for (int j = 0; j < gridPointsY; j++) {
                //grid[i][j] = new Point(i*this.interPointSeparation_, j*this.interPointSeparation_, 0);
                grid[i][j] = new Point[gridPointsZ];
                for (int k = 0; k < gridPointsZ; k++) {
                    grid[i][j][k] = new Point(this, i, j, k);
                }
            }
        }

        // Generate the propagations regions and the Voronoi partition
        propRegions = new ArrayList<>();
        for (int i = 0; i < numPropagationRegions; i++) {
            int x = random.nextInt(gridPointsX);
            int y = random.nextInt(gridPointsY);
            propRegions.add(new Region(i, x, y));
        }
        computeVoronoiPropagationRegion(propRegions);

        // Load cells
        loadCells(cellConfigFile, scenario);
    }

    /**
     * Load the main parameters of the instance
     *
     * @param propFile The filename of the configuration file
     */
    private void loadMainParameters(String propFile) {

        System.out.println("Loading main config file...");

        Properties pro = new Properties();
        try (InputStream resourceStream = getClass().getResourceAsStream("/common/" + propFile)) {
            pro.load(resourceStream);
        } catch (Exception e) {
            System.out.println("Error loading properties " + propFile);
            System.exit(-1);
        }

        this.gridPointsX = Integer.parseInt(pro.getProperty("gridPointsX"));
        this.gridPointsY = Integer.parseInt(pro.getProperty("gridPointsY"));
        this.gridPointsZ = Integer.parseInt(pro.getProperty("gridPointsZ"));
        this.interPointSeparation = Integer.parseInt(pro.getProperty("interPointSeparation"));
        terrainWidth = gridPointsX * interPointSeparation;
        terrainHeight = gridPointsY * interPointSeparation;
        seed = Long.parseLong(pro.getProperty("seed", "2889123676182312233221"));
        this.numPropagationRegions = Integer.parseInt(pro.getProperty("propagationRegions"));
        this.binaryMatrixFile = pro.getProperty("binaryMatrixFile");
        this.cellConfigFile = pro.getProperty("cellConfigFile");
        this.operatorsFile = pro.getProperty("operatorsFile");
        this.hetNetConfigFile = pro.getProperty("hetNetConfigFile");
        this.staticUserConfigFile = pro.getProperty("staticUserConfigFile");
        this.mobilityType = pro.getProperty("mobility");
        this.dynamicUserConfigFile = pro.getProperty("dynamicUserConfigFile");
        this.mobilityConfigFile = pro.getProperty("mobilityConfigFile");
        this.mobilityMatrixFile = pro.getProperty("matrixFile");
    }

    /**
     * Load the different configurations of the difference cells in the UDN
     *
     * @param propFile The filename of the configuration file
     * @param scenario The scenario type
     */
    private void loadCells(String propFile, String scenario) {

        System.out.println("Loading cells config file...");

        Properties pro = new Properties();
        try (InputStream resourceStream = getClass().getResourceAsStream("/common/" + propFile)) {
            pro.load(resourceStream);
        } catch (IOException e) {
            System.out.println("Error loading properties " + propFile);
            System.exit(-1);
        }

        int numberOfCellTypes = Integer.parseInt(pro.getProperty("cellTypes", "3"));

        this.cells = new HashMap<>();
        this.btss = new HashMap<>();
        this.cellOrder = new ArrayList<>();
        this.btsList = new ArrayList<>();
        this.sectorList = new ArrayList<>();
        this.towerList = new HashMap<>();
        this.lastfreq = 0;

        for (int i = 0; i < numberOfCellTypes; i++) {
            loadCellConfig("/scenarios/" + scenario + "/" + pro.getProperty("cell" + i));
        }
        int totalCells = 0;
        int totalBS = 0;

        for (double d : cells.keySet()) {
            totalCells += cells.get(d).size();
            totalBS += btss.get(d).size();
        }
        System.out.println("\tTotal cells: " + totalCells + ", Total BS: " + totalBS);
    }

    /**
     * Load the configuration of a particular cell type
     *
     * @param cellPropFile The filename of the configuration file
     */
    private void loadCellConfig(String cellPropFile) {

        System.out.println("Loading cell config file " + cellPropFile);

        Properties pro = new Properties();
        try (InputStream resourceStream = getClass().getResourceAsStream(cellPropFile)) {
            pro.load(resourceStream);
        } catch (IOException e) {
            System.out.println("Error loading properties " + cellPropFile);
            System.exit(-1);
        }

        //load parameters
        //load cell name
        String cellTypeName = pro.getProperty("type");
        String cellname = pro.getProperty("name");
        String radiationPatternFile = pro.getProperty("radiationPatternFile");

        //loading propagation parameters
        int numSectors = Integer.parseInt(pro.getProperty("numSectors", "3"));
        int numChainsTX = Integer.parseInt(pro.getProperty("numRFChains", "1"));
        double transmittedPower = Double.parseDouble(pro.getProperty("transmittedPower", "50"));
        double alfa = Double.parseDouble(pro.getProperty("alfa", "21"));
        double beta = Double.parseDouble(pro.getProperty("beta", "344"));
        double delta = Double.parseDouble(pro.getProperty("delta", "2"));
        double transmitterGain = Double.parseDouble(pro.getProperty("transmitterGain", "14"));
        double receptorGain = Double.parseDouble(pro.getProperty("receptorGain", "1"));
        double workingFrequency = Double.parseDouble(pro.getProperty("workingFrequency", "2.1e6"));
        double coverageRadius = Double.parseDouble(pro.getProperty("coverageRadius", "2.1e6"));
        double[] singularValuesH = Arrays.stream(pro.getProperty("singularValuesH", "1").split(",")).mapToDouble(Double::parseDouble).toArray();

        //load the number of cells of with this configuration
        int numCells = Integer.parseInt(pro.getProperty("numCells", "10"));
        double lambda = Double.parseDouble(pro.getProperty("lambdaForPPP", "50"));
        double mu = this.gridPointsY * this.gridPointsX * this.interPointSeparation * this.interPointSeparation;
        mu = mu / (1000000.0);

        //uncomment for PPP distributions
        PPP ppp = new PPP(random);
        int numBtss = ppp.getPoisson(lambda * mu);
        numBtss = (int) Math.ceil((double) numBtss / (numSectors * numChainsTX));

        if (cellTypeName.equals("macro")) {
            numBtss = 0;
        }

        System.out.println("\t" + cellTypeName + ": " + numBtss * numSectors * numChainsTX + ", numBTSs: " + numBtss);

        int totalRFChains = numSectors * numChainsTX;
        List<Cell> cells = new ArrayList<>();
        List<BTS> btss = new ArrayList<>();

        //add antennas of new type in already existing positions
        if (lastfreq != 0) {
            for (BTS b : this.btss.get(lastfreq)) {
                //create a new BTS of the current frequency in this point
                int x, y, z;
                x = b.getX();
                y = b.getY();
                //height = 5m for femto and pico and 10 m for micro and macro
                z = (cellTypeName.equalsIgnoreCase("femto") || cellTypeName.equalsIgnoreCase("pico")) ? 1 : 2;

                BTS bts = new BTS(x, y, z, cellTypeName, workingFrequency, radiationPatternFile, this, cellname);
                btsList.add(bts);

                bts.addSectors(cellTypeName, cellname, numChainsTX, numSectors, transmittedPower, alfa, beta, delta, transmitterGain, receptorGain, workingFrequency, coverageRadius);
                sectorList.addAll(bts.getSectors());

                // Add BTS to tower map

                List<BTS> tmp = towerList.get(x + "-" + y);
                tmp.add(bts);
                towerList.put(x + "-" + y, tmp);


                //set cell (RF Chain) angle shift depending on BTS and antenna array configuration
                int cellIndex = 0;
                int initialDegree = random.nextInt(360);
                int angleShift = 360 / totalRFChains;
                for (Sector sector : bts.getSectors()) {
                    for (Cell cell : sector.getCells()) {
                        cell.setSector(sector);

                        if (cellIndex == 0) {
                            cell.setAngleShift(initialDegree);
                        } else {
                            cell.setAngleShift(((cellIndex * angleShift) + initialDegree) % 360);
                        }

                        cell.setSingularValuesH(singularValuesH);

                        cells.add(cell);
                        cellOrder.add(new int[]{cell.getSector().getX(), cell.getSector().getY(), cell.getSector().getZ()});
                        cellIndex++;
                    }
                }
                //add the bts to the map
                btss.add(bts);
                grid[x][y][z].addInstalledBTS(workingFrequency, bts);

                if (btss.size() >= numBtss) { //for security (never enter here)
                    break;
                }
            }
        }//if

        for (int c = btss.size(); c < numBtss; c++) {
            int x, y, z;

            //TODO rerandomize if point has BTS installed
            x = random.nextInt(gridPointsX);
            y = random.nextInt(gridPointsY);

            //height = 5m for femto and pico and 10 m for micro and macro
            z = (cellTypeName.equalsIgnoreCase("femto") || cellTypeName.equalsIgnoreCase("pico")) ? 1 : 2;

            BTS bts = new BTS(x, y, z, cellTypeName, workingFrequency, radiationPatternFile, this, cellname);
            btsList.add(bts);

            bts.addSectors(cellTypeName, cellname, numChainsTX, numSectors, transmittedPower, alfa, beta, delta, transmitterGain, receptorGain, workingFrequency, coverageRadius);
            sectorList.addAll(bts.getSectors());

            // Add BTS to tower map
            List<BTS> tmp = new ArrayList<>();
            tmp.add(bts);
            towerList.put(x + "-" + y, tmp);

            //set cell (RF Chain) angle shift depending on BTS and antenna array configuration
            int cellIndex = 0;
            int initialDegree = random.nextInt(360);
            int angleShift = 360 / totalRFChains;
            for (Sector sector : bts.getSectors()) {
                for (Cell cell : sector.getCells()) {
                    cell.setSector(sector);

                    if (cellIndex == 0) {
                        cell.setAngleShift(initialDegree);
                    } else {
                        cell.setAngleShift(((cellIndex * angleShift) + initialDegree) % 360);
                    }

                    cell.setSingularValuesH(singularValuesH);

                    cells.add(cell);
                    cellOrder.add(new int[]{cell.getSector().getX(), cell.getSector().getY(), cell.getSector().getZ()});
                    cellIndex++;
                }
            }

            //add the bts to the map
            btss.add(bts);
            grid[x][y][z].addInstalledBTS(workingFrequency, bts);
        }

        lastfreq = workingFrequency;

        List<Cell> previousCells = this.cells.putIfAbsent(workingFrequency, cells);
        if (previousCells != null) {
            previousCells.addAll(cells);
            this.cells.put(workingFrequency, previousCells);
        }

        List<BTS> previousBTSs = this.btss.putIfAbsent(workingFrequency, btss);
        if (previousBTSs != null) {
            previousBTSs.addAll(btss);
            this.btss.put(workingFrequency, previousBTSs);
        }
    }

    /**
     * Computes a Voronoi teselation based on a giver set of propagation regions
     */
    private void computeVoronoiPropagationRegion(List<Region> regions) {
        //for each point in the grid, find the closest Region
        for (int i = 0; i < gridPointsX; i++) {
            for (int j = 0; j < gridPointsY; j++) {
                Region closestRegion = null;
                double d, maxDistance = Double.MAX_VALUE;

                for (Region r : regions) {
                    d = distance2D(grid[i][j][0].x, grid[i][j][0].y, r.x, r.y);
                    if (d < maxDistance) {
                        maxDistance = d;
                        closestRegion = r;
                    }
                }

                //Set prop Region for all points with the given x,y coordinates
                for (Point p : grid[i][j]) {
                    p.setPropagationRegion(closestRegion);
                }
            }
        }
    }

    /**
     * Return closest cell by cell type and best sector.
     *
     * @param p
     * @param type
     * @return
     */
    public Cell getClosestCellByType(Point p, CellType type) {
        double min = Double.POSITIVE_INFINITY;
        List<Cell> closestCells = new ArrayList<>();

        for (double frequency : this.cells.keySet()) {
            if (!cells.get(frequency).isEmpty() && cells.get(frequency).get(0).getType() == type) {
                for (Cell c : cells.get(frequency)) {
                    double distance = distance2D(p.x, p.y, c.getBTS().getX(), c.getBTS().getY());

                    if (distance < min) {
                        min = distance;
                        closestCells.clear();
                        closestCells.add(c);
                    } else if (distance == min && !closestCells.contains(c)) {
                        closestCells.add(c);
                    }
                }
            }
        }

        Cell closest = null;
        double max_sinr = Double.NEGATIVE_INFINITY;
        for (Cell c : closestCells) {
            double sinr = getGridPoint(p.x, p.y, p.z).computeSNR(c);
            if (sinr > max_sinr) {
                max_sinr = sinr;
                closest = c;
            }
        }

        return closest;
    }

    public String getMobilityType() {
        return mobilityType;
    }

    /**
     * Return the grid used to discretize the terrain
     *
     * @return Grid
     */
    public Point[][][] getGrid() {
        return this.grid;
    }

    /**
     * Print debug information
     */
    public void printGrid() {
        String ANSI_RESET = "\u001B[0m";

        String ANSI_BLACK = "\u001B[30m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_BLUE = "\u001B[34m";
        String ANSI_PURPLE = "\u001B[35m";
        String ANSI_CYAN = "\u001B[36m";
        String ANSI_WHITE = "\u001B[37m";

        String ANSI_BLACK_BACKGROUND = "\u001B[40m";
        String ANSI_RED_BACKGROUND = "\u001B[41m";
        String ANSI_GREEN_BACKGROUND = "\u001B[42m";
        String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
        String ANSI_BLUE_BACKGROUND = "\u001B[44m";
        String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
        String ANSI_CYAN_BACKGROUND = "\u001B[46m";
        String ANSI_WHITE_BACKGROUND = "\u001B[47m";

        String macroBack = ANSI_RED_BACKGROUND;
        String macroColor = ANSI_RED;
        String microBack = ANSI_GREEN_BACKGROUND;
        String microColor = ANSI_GREEN;
        String picoBack = ANSI_PURPLE_BACKGROUND;
        String picoColor = ANSI_PURPLE;
        String femtoBack = ANSI_CYAN_BACKGROUND;
        String femtoColor = ANSI_CYAN;

//        int count = 0;
//        for (double d : btss_.keySet()) {
//            count += btss_.get(d).size();
//            for (BTS b : btss_.get(d)) {
//                System.out.println(b.getX() + " " + b.getY());
//            }
//        }

//        System.out.println("BTSs: " + count);

        // Print the cells of all BTSs with with its positions in the grid
        // ID [x, y] TYPE

//        System.out.println();
//        for (BTS b : this.btss_) {
//            System.out.println("BTS ID: " + b.getId());
//            for (double d : b.getAntennas().keySet()) {
//                for (Antenna a : b.getAntennas().get(d)) {
//                    for (Cell c : a.getCells()) {
//                        System.out.println("\tCell ID: " + c.getID() + "\t[" + b.getX() + ", " + b.getY() + "] " + c.getType());
//                    }
//                }
//            }
//        }

        int offset = 4;
        String[][] matrix = new String[gridPointsX * offset / 2][gridPointsY];

        for (String[] strings : matrix) {
            Arrays.fill(strings, String.format("%1$" + offset + "s", ""));
        }

        for (int i = 0; i < matrix.length - 1; i += offset / 2) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = "  + ";
                matrix[i + 1][j] = "  + ";
            }
        }

        for (int i = 0; i < gridPointsX; i++) {
            for (int j = 0; j < gridPointsY; j++) {
                for (int k = 0; k < 5; k++) {
                    if (grid[i][j][k].hasBTSInstalled()) {
                        List<Cell> cells = grid[i][j][k].getCells();
//                        System.out.println("[" + i + ", " + j + "]" + grid[i][j][k].getActiveCells().size() + " " + cells.size());

                        // Check BTS cell types
                        String background = macroBack;
                        String color = macroColor;
                        for (Cell c : cells) {
                            switch (c.getType()) {
                                case MICRO:
                                    background = microBack;
                                    color = microColor;
                                    break;
                                case PICO:
                                    background = picoBack;
                                    color = picoColor;
                                    break;
                                case FEMTO:
                                    background = femtoBack;
                                    color = femtoColor;
                                    break;
                            }
                            if (c.getType().equals(CellType.FEMTO)) {
                                break;
                            }
                        }

                        Object[] freqArray = grid[i][j][k].getInstalledBTS().keySet().toArray();
                        double freq = (Double) freqArray[freqArray.length - 1];
                        if (!grid[i][j][k].getActiveCells().isEmpty()) {
                            matrix[i * offset / 2][j] = background + ANSI_BLACK + " BTS" + ANSI_RESET;
                            matrix[(i * offset / 2) + 1][j] = String.format(background + ANSI_BLACK + "%1$" + offset + "s" + ANSI_RESET, grid[i][j][k].getInstalledBTS().get(freq).getId());
                        } else {
                            matrix[i * offset / 2][j] = color + " BTS" + ANSI_RESET;
                            matrix[(i * offset / 2) + 1][j] = String.format(color + "%1$" + offset + "s" + ANSI_RESET, grid[i][j][k].getInstalledBTS().get(freq).getId());
                        }
                        break;
                    }
                }
            }
        }

        users.forEach(user -> {
            int x = user.getX() * offset / 2;

            // Print the user ID and the serving cell ID
//            System.out.println("UE ID: " + user.getID() + "\tCell ID: " + user.getServingCell().getID() + " " + user.getServingCell().getType());

            String background;
            String color;
            switch (user.getServingCell().getType()) {
                case MACRO:
                    background = macroBack;
                    color = macroColor;
                    break;
                case MICRO:
                    background = microBack;
                    color = microColor;
                    break;
                case PICO:
                    background = picoBack;
                    color = picoColor;
                    break;
                default:    // FEMTO
                    background = femtoBack;
                    color = femtoColor;
                    break;
            }

            if (user.isActive()) {
                matrix[x][user.getY()] = String.format(background + ANSI_BLACK + "%1$" + offset + "s" + ANSI_RESET, user.getServingCell().getBTS().getId());
                matrix[x + 1][user.getY()] = String.format(background + ANSI_BLACK + "%1$" + offset + "s" + ANSI_RESET, user.getServingCell().getID());
            } else {
                matrix[x][user.getY()] = String.format(color + "%1$" + offset + "s" + ANSI_RESET, user.getServingCell().getBTS().getId());
                matrix[x + 1][user.getY()] = String.format(color + "%1$" + offset + "s" + ANSI_RESET, user.getServingCell().getID());
            }
        });

        System.out.println("\nBTS macro\t-\tUE to macro\t\t\t\t\t\t" + macroBack + "    " + ANSI_RESET);
        System.out.println("BTS micro\t-\tUE to micro\t\t\t\t\t\t" + microBack + "    " + ANSI_RESET);
        System.out.println("BTS pico\t-\tUE to pico\t\t\t\t\t\t" + picoBack + "    " + ANSI_RESET);
        System.out.println("BTS femto\t-\tUE to femto\t\t\t\t\t\t" + femtoBack + "    " + ANSI_RESET);

        for (String[] strings : matrix) {
            for (String string : strings) {
                System.out.print(string);
            }
            System.out.println();
        }

        for (double d : btss.keySet()) {
            for (BTS b : btss.get(d)) {
                System.out.print("BTS ID " + b.getId() + "\t->\t");
                for (Sector s : b.getSectors()) {
                    for (Cell c : s.getCells()) {
                        System.out.print(c.getID() + " ");
                    }
                }
                System.out.println();
            }
        }

        System.out.println("Femto: " + usersConnectedToFemto() + "\tPico: " + usersConnectedToPico() + "\tMicro: " + usersConnectedToMicro() + "\n");
    }


    /**
     * Print debug information
     */
    public void printGridNew() {
        String ANSI_RESET = "\u001B[0m";

        String ANSI_BLACK = "\u001B[30m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_BLUE = "\u001B[34m";
        String ANSI_PURPLE = "\u001B[35m";
        String ANSI_CYAN = "\u001B[36m";
        String ANSI_WHITE = "\u001B[37m";

        String ANSI_BLACK_BACKGROUND = "\u001B[40m";
        String ANSI_RED_BACKGROUND = "\u001B[41m";
        String ANSI_GREEN_BACKGROUND = "\u001B[42m";
        String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
        String ANSI_BLUE_BACKGROUND = "\u001B[44m";
        String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
        String ANSI_CYAN_BACKGROUND = "\u001B[46m";
        String ANSI_WHITE_BACKGROUND = "\u001B[47m";

        String macroBack = ANSI_RED_BACKGROUND;
        String macroColor = ANSI_RED;
        String microBack = ANSI_GREEN_BACKGROUND;
        String microColor = ANSI_GREEN;
        String picoBack = ANSI_PURPLE_BACKGROUND;
        String picoColor = ANSI_PURPLE;
        String femtoBack = ANSI_CYAN_BACKGROUND;
        String femtoColor = ANSI_CYAN;
        String towerBack = ANSI_YELLOW_BACKGROUND;

        int offset = 4;
        String[][] matrix = new String[gridPointsX * offset / 2][gridPointsY];

        for (String[] strings : matrix) {
            Arrays.fill(strings, String.format("%1$" + offset + "s", ""));
        }

        for (int i = 0; i < matrix.length - 1; i += offset / 2) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = "  + ";
                matrix[i + 1][j] = "  + ";
            }
        }

        //TODO This map has to be changed if BTSs with different heights are considered
//        List<Point> Towers = new ArrayList<>();
//        
//        for (int i = 0; i < gridPointsX_; i++) {
//            for (int j = 0; j < gridPointsY_; j++) {
//                for (int k = 0; k < gridPointsZ_; k++) {
//                    if (grid[i][j][k].hasBTSInstalled()) {
//                        String textColor =  ANSI_BLACK;
//                        if(grid[i][j][k].getActiveCells().isEmpty()){
//                            //There are no active cells
//                            textColor = ANSI_WHITE;
//                        }
//                        Towers.add(grid[i][j][k]);
//                        matrix[i * offset / 2][j] = towerBack + textColor + " TOW" + ANSI_RESET;
//                        matrix[(i * offset / 2) + 1][j] = String.format(towerBack + textColor + "%1$" + offset + "s" + ANSI_RESET, Towers.indexOf(grid[i][j][k]));
//                        
////                        matrix[i * offset / 2][j] =  String.format(towerBack + textColor + "%1$" + offset + "s" + ANSI_RESET, " TOW");
////                        matrix[(i * offset / 2) + 1][j] = String.format(towerBack + textColor + "%1$" + offset + "s" + ANSI_RESET, Towers.indexOf(grid[i][j][k]));
//                        //break;
//                    }
//                }
//            }
//        }

        List<Point> Towers = new ArrayList<>();
        boolean installed = false;
        boolean active = true;

        for (int i = 0; i < gridPointsX; i++) {
            for (int j = 0; j < gridPointsY; j++) {
                for (int k = 0; k < gridPointsZ; k++) {
                    if (grid[i][j][k].hasBTSInstalled()) {
                        installed = true;
                        if (grid[i][j][k].getActiveCells().isEmpty()) {
                            //There are no active cells
                            active = false;
                        }

                    }

                }
                if (installed) {
                    String textColor = ANSI_BLACK;
                    if (!active) {
                        //There are no active cells
                        textColor = ANSI_WHITE;
                    }
                    Point p = grid[i][j][0];
                    Towers.add(p);
                    matrix[i * offset / 2][j] = towerBack + textColor + " TOW" + ANSI_RESET;
                    matrix[(i * offset / 2) + 1][j] = String.format(towerBack + textColor + "%1$" + offset + "s" + ANSI_RESET, Towers.indexOf(p));
                }
                installed = false;
                active = true;
            }
        }

        users.forEach(user -> {
            int x = user.getX() * offset / 2;
            BTS b = user.getServingCell().getBTS();
            Point bp = grid[b.getX()][b.getY()][b.getZ()];
            Point bp_2D = grid[b.getX()][b.getY()][0];

            // Print the user ID and the serving cell ID
//            System.out.println("UE ID: " + user.getID() + "\tCell ID: " + user.getServingCell().getID() + " " + user.getServingCell().getType());

            String background;
            String color;
            switch (user.getServingCell().getType()) {
                case MACRO:
                    background = macroBack;
                    color = macroColor;
                    break;
                case MICRO:
                    background = microBack;
                    color = microColor;
                    break;
                case PICO:
                    background = picoBack;
                    color = picoColor;
                    break;
                default:    // FEMTO
                    background = femtoBack;
                    color = femtoColor;
                    break;
            }

            if (user.isActive()) {
                boolean check = Towers.contains(bp_2D);
                int check2 = Towers.indexOf(bp_2D);

                matrix[x][user.getY()] = String.format(background + ANSI_BLACK + "%1$" + offset + "s" + ANSI_RESET, "T" + Towers.indexOf(bp_2D));
                //matrix[x + 1][user.getY()] = String.format(background + ANSI_BLACK + "%1$" + offset + "s" + ANSI_RESET, user.getServingCell().getBTS().getId());
                matrix[x + 1][user.getY()] = String.format(background + ANSI_BLACK + "%1$" + offset + "s" + ANSI_RESET, user.getServingCell().getBTS().getId());
            } else {
                matrix[x][user.getY()] = String.format(color + "%1$" + offset + "s" + ANSI_RESET, "T" + Towers.indexOf(bp_2D));
                matrix[x + 1][user.getY()] = String.format(color + "%1$" + offset + "s" + ANSI_RESET, "U" + user.getID());
            }
        });

        System.out.println("\nUE to macro\t\t\t\t\t\t" + macroBack + "    " + ANSI_RESET);
        System.out.println("UE to micro\t\t\t\t\t\t" + microBack + "    " + ANSI_RESET);
        System.out.println("UE to pico\t\t\t\t\t\t" + picoBack + "    " + ANSI_RESET);
        System.out.println("UE to femto\t\t\t\t\t\t" + femtoBack + "    " + ANSI_RESET);

        for (String[] strings : matrix) {
            for (String string : strings) {
                System.out.print(string);
            }
            System.out.println();
        }

//        for (double d : btss_.keySet()) {
//            for (BTS b : btss_.get(d)) {
//                System.out.print("BTS ID " + b.getId() + "\t->\t");
//                for (Sector s : b.getSectors()) {
//                    for (Cell c : s.getCells()) {
//                        System.out.print(c.getID() + " ");
//                    }
//                }
//                System.out.println();
//            }
//        }

        for (Point p : Towers) {
            //for(int j = 0; j<Towerss.size(); j++){
            //Point p = Towers.get(j);
            System.out.print("TOW " + Towers.indexOf(p) + "\t->\t");
            for (int k = 0; k < gridPointsZ; k++) {
                Point pcheck = grid[p.x][p.y][k];
                for (BTS b : pcheck.getInstalledBTS().values()) {

                    System.out.print("BTS " + b.getId() + "(" + b.getSectors().get(0).getCells().get(0).printType() + ") ");

                }

            }
            System.out.println();

        }

        System.out.println("Femto: " + usersConnectedToFemto() + "\tPico: " + usersConnectedToPico() + "\tMicro: " + usersConnectedToMicro() + "\n");
    }


    /**
     * Print user information
     */
    void printUsers() {
        System.out.println("Printing information of " + this.users.size() + " users:");
        System.out.println(this.users);
    }

    /**
     * Returns the list or users in the UDN
     *
     * @return The list of users
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Number of total cells in the UDN
     *
     * @return The total number of cells in the UDN
     */
    public int getTotalNumberOfCells() {
        int count = 0;

        for (List<Cell> c : this.cells.values()) {
            count += c.size();
        }

        return count;
    }

    /**
     * Return the number of activable cells, i.e., all but macros
     *
     * @return The total number of activable cells in the UDN
     */
    public int getTotalNumberOfActivableCells() {
        int count = 0;

        for (List<Cell> c : this.cells.values()) {
            if ((c.size() > 0) && (c.get(0).getType() != CellType.MACRO)) {
                count += c.size();
            }
        }

        return count;
    }

    /**
     * Getter of a point of the grid
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return A point of the grid used to discretize the working area
     */
    public Point getGridPoint(int x, int y, int z) {
        return this.grid[x][y][z];
    }

    /**
     * Getter of the random number
     *
     * @return Random number
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Getter of the inter point separation of the grid
     *
     * @return Inter point separation of the grid
     */
    public double getInterpointSeparation() {
        return this.interPointSeparation;
    }

    /**
     * Activate/deactivates BTSs according to the information enclosed in the binary solution of the
     * problem
     *
     * @param cso
     */
    public void setCellActivation(BitSet cso) {
        int bts = 0;

        for (List<Cell> cells : this.cells.values()) {
            for (Cell c : cells) {
                if (c.getType() != CellType.MACRO) {
                    c.setActivation(cso.get(bts));
                    bts++;
                }
            }
        }
    }

    /**
     * Set the activation/deactivation plan of a binary string into the UDN
     *
     * @param cso A binary string containing whether the cell is active or not
     */
    public void copyCellActivation(BitSet cso) {
        int bts = 0;

        for (List<Cell> cells : this.cells.values()) {
            for (Cell c : cells) {
                if (c.getType() != CellType.MACRO) {
                    //c.setActivation(cso.getIth(bts));
                    cso.set(bts, c.isActive());  //TODO revisar si esto es correcto
                    bts++;
                }
            }
        }
    }

    /**
     * Computes the number of grid points with the different stats computed
     *
     * @return The number of points
     */
    public int pointsWithStatsComputed() {
        int count = 0;
        for (int i = 0; i < this.gridPointsX; i++) {
            for (int j = 0; j < this.gridPointsY; j++) {
                for (int k = 0; k < this.gridPointsZ; k++) {
                    if (grid[i][j][k].statsComputed) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Number of active cells in the UDN
     *
     * @return The number of active cells
     */
    public int getTotalNumberOfActiveCells() {
        int count = 0;

        for (List<Cell> cells : this.cells.values()) {
            for (Cell c : cells) {
                if (c.isActive()) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Computes the total received power at the grid points where a user is placed
     */
    public void computeSignaling() {
        //computes for all as it is used in the solution initialization
        for (User u : this.users) {
            int i = u.getX();
            int j = u.getY();
            int k = u.getZ();

            grid[i][j][k].computeTotalReceivedPower();
        }
    }

    /**
     * Set to 0 the number of users assigned to all the cells of the UDN
     */
    public void resetNumberOfUsersAssignedToCells() {
        for (List<Cell> cells : this.cells.values()) {
            for (Cell c : cells) {
                c.setNumbersOfUsersAssigned(0);
            }
        }
    }

    /**
     * Checks that all the users have been assigned to a cell
     */
    public void validateUserAssigned() {
        int users = 0;
        for (List<Cell> cells : this.cells.values()) {
            for (Cell c : cells) {
                users += c.getAssignedUsers();
            }
        }

        if (users != this.users.size()) {
            System.out.println("Error when assgning users.");
            System.exit(-1);
        }

    }

    /**
     * 2D distance in the grid
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public double distance2D(int x1, int y1, int x2, int y2) {
        double d = 0.0;

        x1 = x1 * this.interPointSeparation;
        y1 = y1 * this.interPointSeparation;
        x2 = x2 * this.interPointSeparation;
        y2 = y2 * this.interPointSeparation;

        if ((x1 == x2) && (y1 == y2)) {
            return 0.1 * this.interPointSeparation;
        } else {

            d += (x1 - x2) * (x1 - x2);
            d += (y1 - y2) * (y1 - y2);
            d = Math.sqrt(d);

            return d;
        }
    }

    /**
     * Computes the Euclidean distance between two points in the grid
     *
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @return Euclidian distance between two points
     */
    public double distance(int x1, int y1, int z1, int x2, int y2, int z2) {
        double d = 0.0;

        x1 = x1 * this.interPointSeparation;
        y1 = y1 * this.interPointSeparation;
        z1 = z1 * this.interPointSeparation;
        x2 = x2 * this.interPointSeparation;
        y2 = y2 * this.interPointSeparation;
        z2 = z2 * this.interPointSeparation;

        if ((x1 == x2) && (y1 == y2) && (z1 == z2)) {
            return 0.1 * this.interPointSeparation;
        } else {

            d += (x2 - x1) * (x2 - x1);
            d += (y2 - y1) * (y2 - y1);
            d += (z2 - z1) * (z2 - z1);
            d = Math.sqrt(d);

            return d;
        }
    }

    /**
     * Given a point and a BTS, this function calculates both the azimuthal and the occipital angles
     * (seen by the BTS)
     *
     * @param p
     * @param bts
     * @return
     */
    public int[] calculateAngles(Point p, BTS bts) {
        double occirad; //occipithal angle (theta)
        double azirad; //azimithal angle (phi)
        int occi, azi;

        Point bts_p = bts.getPoint();
        double d3 = distance(p.x, p.y, p.z, bts_p.x, bts_p.y, bts_p.z);
        double d2 = distance2D(p.x, p.y, bts_p.x, bts_p.y);
        int xDistance = abs(p.x - bts_p.x) * this.interPointSeparation;
        int yDistance = abs(p.y - bts_p.y) * this.interPointSeparation;
        int zDistance = abs(p.z - bts_p.z) * this.interPointSeparation;

        occirad = acos(zDistance / d3);
        occi = 180 - (int) Math.toDegrees(occirad);

        if (p.x >= bts_p.x) {
            if (p.y >= bts_p.y) {
                //azi between 0 and 90 deg
                azirad = acos(xDistance / d2);
                azi = (int) Math.toDegrees(azirad);
            } else {
                //azi between 270 and 360 deg
                azirad = acos(yDistance / d2);
                azi = 270 + (int) Math.toDegrees(azirad);
            }
        } else {
            if (p.y >= bts_p.y) {
                //azi between 90 and 180 deg
                azirad = acos(yDistance / d2);
                azi = 90 + (int) Math.toDegrees(azirad);

            } else {
                //azi between 180 and 270 deg
                azirad = acos(xDistance / d2);
                azi = 180 + (int) Math.toDegrees(azirad);
            }
        }

        return new int[]{azi, occi};
    }

    public void getCellsOfInterestByPoint() {
//        FileWriter myWriter = null;
//        try {
//            myWriter = new FileWriter("potencias.txt");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        cellsOfInterestByPoint = new HashMap<>();
//        for (int i = 0; i < gridPointsX_; i++) {
//            for (int j = 0; j < gridPointsY_; j++) {
//                Point p = getGridPoint(i, j, 0);
//                cellsOfInterestByPoint.put(p, new HashMap<>());
//                for (Double f : cells_.keySet()) {
//                    cellsOfInterestByPoint.get(p).put(f, new ArrayList<>());
//                    for (Cell c : cells_.get(f)) {
//                        if (Math.pow(10.0, p.computeSignalPower(c) / 10) > 1)
//                            cellsOfInterestByPoint.get(p).get(f).add(c);
//                    }
//                }
//
//                try {
//                    boolean hay = false;
//                    for (double d : cellsOfInterestByPoint.get(p).keySet()) {
//                        if (!cellsOfInterestByPoint.get(p).get(d).isEmpty())
//                            hay = true;
//                    }
//
//                    if (hay) {
//                        myWriter.write("1 ");
//                    } else
//                        myWriter.write("0 ");
////                                myWriter.write(Math.pow(10.0, p.computeSignalPower(c) / 10) + "\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            try {
//                myWriter.write("\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        System.exit(0);
    }

    /**
     * Randomly allocate the positions of the users
     */
    public void updateUsersPosition() {
        for (User u : users) {
            int x = random.nextInt(gridPointsX);
            int y = random.nextInt(gridPointsY);
            u.setX(x);
            u.setY(y);
        }
    }

    /**
     * Updates the demands of all the users
     */
    public void updateUsersDemand() {
        for (User u : this.users) {
            u.updateDemand();
        }
    }

    /**
     * Computes the users whose serving BTS is the macrocell
     *
     * @return
     */
    public int usersConnectedToMacro() {
        int count = 0;
        for (User u : this.users) {
            if (u.getServingCell().getType() == CellType.MACRO) {
                count++;
            }
        }
        return count;
    }

    /**
     * Computes the users being served by femtocells
     *
     * @return
     */
    public int usersConnectedToFemto() {
        int count = 0;
        for (User u : this.users) {
            if (u.getServingCell().getType() == CellType.FEMTO) {
                count++;
            }
        }
        return count;
    }

    /**
     * Computes the users being served by microcells
     *
     * @return
     */
    public int usersConnectedToMicro() {
        int count = 0;
        for (User u : this.users) {
            if (u.getServingCell().getType() == CellType.MICRO) {
                count++;
            }
        }
        return count;
    }

    /**
     * Computes the users being served by picocells
     *
     * @return
     */
    public int usersConnectedToPico() {
        int count = 0;
        for (User u : this.users) {
            if (u.getServingCell().getType() == CellType.PICO) {
                count++;
            }
        }
        return count;
    }

    public int getActiveCellsByType(CellType type) {
        int count = 0;
        for (Double d : cells.keySet()) {
            for (Cell c : cells.get(d)) {
                if (c.getType() == type) {
                    count++;
                } else {
                    break;
                }
            }
        }
        return count;
    }

    public int[] getActiveByType(CellType type) {
        int countBS = 0;
        int countSector = 0;
        int countCell = 0;

        for (BTS b : btsList.stream().filter(bts -> bts.getSectors().get(0).getCells().get(0).getType().equals(type)).toList()) {
            List<Sector> activeSectors = b.getActiveSectors();
            if (activeSectors.size() > 0) {
                countBS++;
                countSector += activeSectors.size();
                for (Sector s : activeSectors) {
                    countCell += s.getActiveCells();
                }
            }
        }

        return new int[]{countBS, countSector, countCell};
    }

    public int getNumberUsersAssignmentByType(CellType type) {
        return users.stream().filter(u -> u.getServingCell().getType().equals(type)).toList().size();
    }

    /**
     * Restarts different data structures in all the Points used to store precomputed signal power and
     * SINR. The goal is saving computational time and memory.
     */
    public void emptyMapsAtPoints() {
        for (int i = 0; i < this.gridPointsX; i++) {
            for (int j = 0; j < this.gridPointsY; j++) {
                for (int k = 0; k < this.gridPointsZ; k++) {
                    this.grid[i][j][k].signalPowerMap = new HashMap<>();
                    this.grid[i][j][k].snrMap = new HashMap<>();
                }
            }
        }
    }

    public int getNumberOfActiveCellsByType(CellType type) {
        int count = 0;
        for (Double d : cells.keySet()) {
            for (Cell c : cells.get(d)) {
                if (c.getType() == type && c.isActive()) count++;
            }
        }
        return count;
    }

    public int getNumberOfCellsByType(CellType type) {
        int count = 0;
        for (Double d : cells.keySet()) {
            for (Cell c : cells.get(d)) {
                if (c.getType().equals(type))
                    count++;
            }
        }
        return count;
    }

    public String getOperatorsFile() {
        return operatorsFile;
    }

    public List<SocialAttractor> getSocialAttractors() {
        return socialAttractors;
    }

    public Map<Double, List<Cell>> getCells() {
        return cells;
    }

    public List<int[]> getCellOrder() {
        return cellOrder;
    }

    public List<BTS> getBTSsList() {
        return btsList;
    }

    public List<Sector> getSectorsList() {
        return sectorList;
    }

    public List<List<BTS>> getTowersList() {
        List<List<BTS>> towers = new ArrayList<>(towerList.keySet().size());
        for (String i : towerList.keySet()) {
            towers.add(towerList.get(i));
        }
        return towers;
    }

    public int getNumberOfTowers() {
        return towerList.size();
    }

    public int getNumberOfSectors() {
        return sectorList.size();
    }

    public int getNumberOfBS() {
        return btsList.size();
    }

    /**
     * Cells
     */
    public enum CellType {
        MACRO, MICRO, PICO, FEMTO
    }


}
