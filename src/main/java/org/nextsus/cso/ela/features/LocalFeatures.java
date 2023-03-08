package org.nextsus.cso.ela.features;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.ela.neighborhood.impl.*;
import org.nextsus.cso.model.UDN;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.DominanceComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class LocalFeatures {
    protected final Problem<BinaryCSOSolution> problem;
    protected List<BinaryCSOSolution> solutionSet;
    protected DominanceComparator<BinaryCSOSolution> dominanceComparator;
    protected String walk;
    protected Neighborhood n;
    protected int numberOfThreads;

    public LocalFeatures(Problem<BinaryCSOSolution> problem, List<BinaryCSOSolution> solutionSet, String walk, Neighborhood.NeighborhoodType neighborhoodType, double percentage, int numberOfThreads) {
        this.problem = problem;
        this.solutionSet = solutionSet;
        this.dominanceComparator = new DefaultDominanceComparator<>();
        this.walk = walk;
        this.n = switch (neighborhoodType) {
            case Tower -> new TowerNeighborhood(problem, percentage);
            case BS -> new BSNeighborhood(problem, percentage);
            case Sector -> new SectorNeighborhood(problem, percentage);
            case Cell -> new CellNeighborhood(problem, percentage);
            case Hamming -> new HammingNeighborhood(problem, percentage);
        };
        this.numberOfThreads = numberOfThreads;
    }

    public void execute() throws InterruptedException {
        // Por cada solución
        //      getNeighborhod
        //
        //      Evolvability for multiobjective optimization:
        //          - #inf -> proporción de vecinos dominados
        //          - #sup -> proporción de vecinos que dominan
        //          - #inc -> proporción de vecinos incomparables
        //          - #lnd -> proporción de soluciones no dominadas
        //          - #lsupp -> proporción de soluciones soportadas en el mismo ?¿ proporción de soluciones compatibles ?¿
        //
        //          - hv -> HV promedio que cubre cada vecino
        //          - hvd -> diferencia promedio entre el HV cubierto por cada vecino y el cubierto por la solución actual
        //          - nhv -> HV cubierto por el vecindario completo
        //
        //          - length_aws -> longitud del paseo. Es un buen estimador de #plo (proporción de soluciones PLO)
        //
        //      En el caso de randomwalk: Kendall coefficient tau (métrica de correlación no linear basada en ranking). Ward's hierarchichal clustering
        //          - Valor promedio y primer coeficiente de autocorrelación de las métricas anteriores. Una fuerte correlación en dos pasos
        //          consecutivos implica una tendencia a ser sencillo mejorar localmente mediante exploración del vecindario. Por el contrario,
        //          cuando no hay correlación, es complicado mejorar localmente. Esto caracteriza la dificultad del landscape multiobjetivo.
        //          - f_cor_rws -> Estimación del grado de correlación entre objetivos.

        /*
        TODO
        - Sacar por cada solución y tipo el número de celdas ON/OFF, y número de sectores y BS completamente ON/OFF
        - Número de usuarios asignados a tipos de celdas
         */

        long initialTime = System.currentTimeMillis();

        System.out.println("\n# Computing local landscape features with " + numberOfThreads + " threads #\n");

        double xLowerLimit = 0.0;
        double xUpperLimit = ((StaticCSO) problem).getConsumptionUpperLimit();
        double yLowerLimit = 0.0;
        double yUpperLimit = -((StaticCSO) problem).getCapacityUpperLimit();
        PISAHypervolume hv = new PISAHypervolume(new double[][]{new double[]{xLowerLimit, yUpperLimit}, new double[]{xUpperLimit, yLowerLimit}});

        Map<Integer, Double> inf_avg_map = Collections.synchronizedMap(new TreeMap<>());
        Map<Integer, Double> sup_avg_map = Collections.synchronizedMap(new TreeMap<>());
        Map<Integer, Double> inc_avg_map = Collections.synchronizedMap(new TreeMap<>());
        Map<Integer, Double> lnd_avg_map = Collections.synchronizedMap(new TreeMap<>());
        Map<Integer, Double> lsupp_avg_map = Collections.synchronizedMap(new TreeMap<>());

        Map<Integer, Double> hv_avg_map = Collections.synchronizedMap(new TreeMap<>());
        Map<Integer, Double> hvd_avg_map = Collections.synchronizedMap(new TreeMap<>());
        Map<Integer, Double> nhv_avg_map = Collections.synchronizedMap(new TreeMap<>());

        Map<UDN.CellType, List<Double>> cell_on_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));
        Map<UDN.CellType, List<Double>> cell_off_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));
        Map<UDN.CellType, List<Double>> sector_on_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));
        Map<UDN.CellType, List<Double>> sector_off_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));
        Map<UDN.CellType, List<Double>> bs_on_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));
        Map<UDN.CellType, List<Double>> bs_off_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));
        Map<UDN.CellType, List<Double>> ue_avg_map = Collections.synchronizedMap(new HashMap<>(solutionSet.size()));

        for (UDN.CellType cellType : List.of(UDN.CellType.MICRO, UDN.CellType.PICO, UDN.CellType.FEMTO)) {
            cell_on_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));
            cell_off_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));
            sector_on_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));
            sector_off_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));
            bs_on_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));
            bs_off_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));
            ue_avg_map.put(cellType, Collections.synchronizedList(new ArrayList<>()));

            System.out.println("Cells " + cellType + " = " + ((StaticCSO) problem).getUDN().getNumberOfCellsByType(cellType));
            System.out.println("Sectors " + cellType + " = " + ((StaticCSO) problem).getUDN().getSectorsList().stream().filter(s -> s.getCells().get(0).getType().equals(cellType)).count());
            System.out.println("BS " + cellType + " = " + ((StaticCSO) problem).getUDN().getBTSsList().stream().filter(b -> b.getSectors().get(0).getCells().get(0).getType().equals(cellType)).count());

        }

        List<Thread> threads = new ArrayList<>();
        List<StaticCSO> staticCSOList = Collections.synchronizedList(new ArrayList<>());
        List<Boolean> problemAvailable = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            staticCSOList.add(new StaticCSO((StaticCSO) problem));
            problemAvailable.add(true);
        }

        for (int j = 0; j < solutionSet.size(); j++) {
            final int id = j;
            Runnable runnable = () -> {
                StaticCSO staticCSO = null;
                int staticCSOidx = -1;
                while (staticCSO == null) {
                    for (int i = 0; i < numberOfThreads; i++) {
                        synchronized (problemAvailable) {
                            if (problemAvailable.get(i)) {
                                staticCSOidx = i;
                                staticCSO = staticCSOList.get(i);
                                problemAvailable.set(i, false);
                                break;
                            }
                        }
                    }
                }

                BinaryCSOSolution solution = solutionSet.get(id);
                staticCSO.evaluate(solution);

                // Features from the instance

                for (UDN.CellType cellType : List.of(UDN.CellType.MICRO, UDN.CellType.PICO, UDN.CellType.FEMTO)) {
                    int[] actives = staticCSO.getUDN().getActiveByType(cellType);

                    // BS
                    List<Double> bs_tmp = bs_on_avg_map.get(cellType);
                    bs_tmp.add((double) actives[0]);
                    bs_on_avg_map.put(cellType, bs_tmp);

                    // Sector
                    List<Double> sector_tmp = sector_on_avg_map.get(cellType);
                    sector_tmp.add((double) actives[1]);
                    sector_on_avg_map.put(cellType, sector_tmp);

                    // Cell
                    List<Double> cell_tmp = cell_on_avg_map.get(cellType);
                    cell_tmp.add((double) actives[2]);
                    cell_on_avg_map.put(cellType, cell_tmp);

                    // UE
                    List<Double> ue_tmp = ue_avg_map.get(cellType);
                    ue_tmp.add((double) staticCSO.getUDN().getNumberUsersAssignmentByType(cellType));
                    ue_avg_map.put(cellType, ue_tmp);
                }

                // Features from the neighborhood

                List<BinaryCSOSolution> neighborhood = n.getNeighborhood(solution);
                for (BinaryCSOSolution neighbor : neighborhood)
                    staticCSO.evaluate(neighbor);

                // To obtain the PS
                NonDominatedSolutionListArchive<BinaryCSOSolution> nonDominatedSolutionArchive = new NonDominatedSolutionListArchive<>();
                nonDominatedSolutionArchive.addAll(neighborhood);

                lnd_avg_map.put(id, (double) nonDominatedSolutionArchive.size());
                lsupp_avg_map.put(id, getSupportedSolutions(nonDominatedSolutionArchive.solutions()));

                double[][] frontSolution = solutionListToDoubleMatrix(List.of(solution));
                double solutionHV = hv.compute(NormalizeUtils.normalize(frontSolution, NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(hv.getReferenceFront()), NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(hv.getReferenceFront())));

                List<Double> hv_list = new ArrayList<>();
                List<Double> hvd_list = new ArrayList<>();

                double sup = 0.0;
                double inf = 0.0;
                double inc = 0.0;

                for (BinaryCSOSolution neighbor : neighborhood) {
                    switch (dominanceComparator.compare(neighbor, solution)) {
                        case -1 -> sup++;
                        case 1 -> inf++;
                        default -> inc++;
                    }

                    double[][] frontNeighbor = solutionListToDoubleMatrix(List.of(neighbor));
                    hv_list.add(hv.compute(NormalizeUtils.normalize(frontNeighbor, NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(hv.getReferenceFront()), NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(hv.getReferenceFront()))));
                    hvd_list.add(Math.abs(solutionHV - hv_list.get(hv_list.size() - 1)));
                }
                inf_avg_map.put(id, inf);
                sup_avg_map.put(id, sup);
                inc_avg_map.put(id, inc);

                hv_avg_map.put(id, hv_list.stream().mapToDouble(i -> i).sum() / neighborhood.size());   // Promedio HV de los vecinos
                hvd_avg_map.put(id, hvd_list.stream().mapToDouble(i -> i).sum() / neighborhood.size()); // Diferencia promedio entre el HV de los vecinos y el de la solución actual
                nhv_avg_map.put(id, hv.compute(NormalizeUtils.normalize(solutionListToDoubleMatrix(neighborhood), NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(hv.getReferenceFront()), NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(hv.getReferenceFront()))));

                problemAvailable.set(staticCSOidx, true);
            };
            threads.add(new Thread(runnable, String.valueOf(id)));
        }

        List<Thread> threadsToJoin = new ArrayList<>();
        for (int i = 0; i < threads.size(); i++) {
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

            Thread t = threads.get(i);
            t.start();
            threadsToJoin.add(t);

            if (i % (threads.size() / 10) == 0) System.out.println(i * 100 / threads.size() + " %");
        }

        System.out.println("100 %\n");

        for (Thread t : threadsToJoin)
            t.join();
        threadsToJoin.clear();

        List<Double> inf_avg_list = inf_avg_map.values().stream().toList();
        inf_avg_map.clear();

        List<Double> sup_avg_list = new ArrayList<>(sup_avg_map.values().stream().toList());
        sup_avg_map.clear();

        List<Double> inc_avg_list = new ArrayList<>(inc_avg_map.values().stream().toList());
        inc_avg_map.clear();

        List<Double> lnd_avg_list = new ArrayList<>(lnd_avg_map.values().stream().toList());
        lnd_avg_map.clear();

        List<Double> lsupp_avg_list = new ArrayList<>(lsupp_avg_map.values().stream().toList());
        lsupp_avg_map.clear();

        List<Double> hv_avg_list = new ArrayList<>(hv_avg_map.values().stream().toList());
        hv_avg_map.clear();

        List<Double> hvd_avg_list = new ArrayList<>(hvd_avg_map.values().stream().toList());
        hvd_avg_map.clear();

        List<Double> nhv_avg_list = new ArrayList<>(nhv_avg_map.values().stream().toList());
        nhv_avg_map.clear();

        double inf_avg = inf_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double sup_avg = sup_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double inc_avg = inc_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double lnd_avg = lnd_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double lsupp_avg = lsupp_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double hv_avg = hv_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double hvd_avg = hvd_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double nhv_avg = nhv_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();

        System.out.println("Average ON/OFF");
        for (UDN.CellType cellType : List.of(UDN.CellType.MICRO, UDN.CellType.PICO, UDN.CellType.FEMTO)) {
            double bs_on = bs_on_avg_map.get(cellType).stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double bs_off = ((StaticCSO) problem).getUDN().getBTSsList().stream().filter(b -> b.getSectors().get(0).getCells().get(0).getType().equals(cellType)).toList().size() - bs_on;

            double sector_on = sector_on_avg_map.get(cellType).stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double sector_off = ((StaticCSO) problem).getUDN().getSectorsList().stream().filter(s -> s.getCells().get(0).getType().equals(cellType)).toList().size() - sector_on;

            double cell_on = cell_on_avg_map.get(cellType).stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double cell_off = ((StaticCSO) problem).getUDN().getNumberOfCellsByType(cellType) - cell_on;

            System.out.println("\t" + cellType);
            System.out.println("\t\t#bs_on = " + bs_on);
            System.out.println("\t\t#bs_off = " + bs_off);
            System.out.println("\t\t#sector_on = " + sector_on);
            System.out.println("\t\t#sector_off = " + sector_off);
            System.out.println("\t\t#cell_on = " + cell_on);
            System.out.println("\t\t#cell_off = " + cell_off);
            System.out.println("\t\t#ue = " + ue_avg_map.get(cellType).stream().mapToDouble(i -> i).sum() / solutionSet.size());
        }

        // Print common features between random and adaptive walk
        System.out.println("\n#inf_avg = " + inf_avg);
        System.out.println("#sup_avg = " + sup_avg);
        System.out.println("#inc_avg = " + inc_avg);
        System.out.println("#lnd_avg = " + lnd_avg);
        System.out.println("#lsupp_avg = " + lsupp_avg);
        System.out.println("hv_avg = " + hv_avg);
        System.out.println("hvd_avg = " + hvd_avg);
        System.out.println("nhv_avg = " + nhv_avg);

        if (walk.equals("random")) {
            // f_cor_rws
            double mean_x = 0.0;
            double mean_y = 0.0;
            for (BinaryCSOSolution s : solutionSet) {
                mean_x += s.objectives()[0];
                mean_y += s.objectives()[1];
            }

            mean_x /= solutionSet.size();
            mean_y /= solutionSet.size();

            double cov_xy = 0.0;
            double std_x = 0.0;
            double std_y = 0.0;
            for (BinaryCSOSolution s : solutionSet) {
                std_x += Math.pow(s.objectives()[0] - mean_x, 2);
                std_y += Math.pow(s.objectives()[1] - mean_y, 2);

                cov_xy += (s.objectives()[0] - mean_x) * (s.objectives()[1] - mean_y);
            }

            std_x = Math.sqrt(std_x / solutionSet.size());
            std_y = Math.sqrt(std_y / solutionSet.size());
            cov_xy /= solutionSet.size();

            double f_cor_rws = cov_xy / (std_x * std_y);

            // First auto-correlation coefficients
            List<Double> inf_r1_rws_list = new ArrayList<>(inf_avg_list);
            inf_r1_rws_list.remove(0);
            double inf_r1_rws = computeCorrelation(inf_avg_list, inf_r1_rws_list);

            List<Double> sup_r1_rws_list = new ArrayList<>(sup_avg_list);
            sup_r1_rws_list.remove(0);
            double sup_r1_rws = computeCorrelation(sup_avg_list, sup_r1_rws_list);

            List<Double> inc_r1_rws_list = new ArrayList<>(inc_avg_list);
            inc_r1_rws_list.remove(0);
            double inc_r1_rws = computeCorrelation(inc_avg_list, inc_r1_rws_list);

            List<Double> lnd_r1_rws_list = new ArrayList<>(lnd_avg_list);
            lnd_r1_rws_list.remove(0);
            double lnd_r1_rws = computeCorrelation(lnd_avg_list, lnd_r1_rws_list);

            List<Double> lsupp_r1_rws_list = new ArrayList<>(lsupp_avg_list);
            lsupp_r1_rws_list.remove(0);
            double lsupp_r1_rws = computeCorrelation(lsupp_avg_list, lsupp_r1_rws_list);

            List<Double> hv_r1_rws_list = new ArrayList<>(hv_avg_list);
            hv_r1_rws_list.remove(0);
            double hv_r1_rws = computeCorrelation(hv_avg_list, hv_r1_rws_list);

            List<Double> hvd_r1_rws_list = new ArrayList<>(hvd_avg_list);
            hvd_r1_rws_list.remove(0);
            double hvd_r1_rws = computeCorrelation(hvd_avg_list, hvd_r1_rws_list);

            List<Double> nhv_r1_rws_list = new ArrayList<>(nhv_avg_list);
            nhv_r1_rws_list.remove(0);
            double nhv_r1_rws = computeCorrelation(nhv_avg_list, nhv_r1_rws_list);

            System.out.println("#inf_r1 = " + inf_r1_rws);
            System.out.println("#sup_r1 = " + sup_r1_rws);
            System.out.println("#inc_r1 = " + inc_r1_rws);
            System.out.println("#lnd_r1 = " + lnd_r1_rws);
            System.out.println("#lsupp_r1 = " + lsupp_r1_rws);
            System.out.println("hv_r1 = " + hv_r1_rws);
            System.out.println("hvd_r1 = " + hvd_r1_rws);
            System.out.println("nhv_r1 = " + nhv_r1_rws);
            System.out.println("f_cor = " + f_cor_rws);
        } else if (walk.equals("adaptive")) System.out.println("#walk_length = " + (solutionSet.size() - 1));

        System.out.println("\nTotal computing time: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - initialTime));
    }

    protected double computeCorrelation(List<Double> x, List<Double> y) {
        double mean_x = 0.0;
        double mean_y = 0.0;
        for (int i = 0; i < y.size(); i++) {
            mean_x += x.get(i);
            mean_y += y.get(i);
        }

        mean_x /= solutionSet.size();
        mean_y /= solutionSet.size();

        double cov_xy = 0.0;
        double std_x = 0.0;
        double std_y = 0.0;
        for (int i = 0; i < y.size(); i++) {
            std_x += Math.pow(x.get(i) - mean_x, 2);
            std_y += Math.pow(y.get(i) - mean_y, 2);

            cov_xy += (x.get(i) - mean_x) * (y.get(i) - mean_y);
        }

        std_x = Math.sqrt(std_x / solutionSet.size());
        std_y = Math.sqrt(std_y / solutionSet.size());
        cov_xy /= solutionSet.size();

        return cov_xy / (std_x * std_y);
    }

    protected double getSupportedSolutions(List<BinaryCSOSolution> front) {
        double supp = 0.0;

        for (int i = 0; i < front.size() - 3; i++) {
            BinaryCSOSolution s1 = front.get(i);
            BinaryCSOSolution s2 = front.get(i + 1);
            BinaryCSOSolution s3 = front.get(i);

            for (int j = 0; j < s1.objectives().length; j++) {
                double diff_s2vss1 = s1.objectives()[j] - s2.objectives()[j];
                double diff_s3vss2 = s2.objectives()[j] - s2.objectives()[j];
                double diff_s2vss3 = s2.objectives()[j] - s3.objectives()[j];
                double diff_s1vss2 = s1.objectives()[j] - s2.objectives()[j];

                if (diff_s2vss1 < diff_s3vss2 || diff_s2vss3 < diff_s1vss2) supp++;
            }
        }
        return supp;
    }

    protected double[][] solutionListToDoubleMatrix(List<BinaryCSOSolution> solutionList) {
        double[][] front = new double[solutionList.size()][2];

        for (int i = 0; i < solutionList.size(); i++) {
            front[i][0] = solutionList.get(i).objectives()[0];
            front[i][1] = solutionList.get(i).objectives()[1];
        }

        return front;
    }
}
