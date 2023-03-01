package org.nextsus.cso.ela.features;

import org.nextsus.cso.ela.neighborhood.Neighborhood;
import org.nextsus.cso.ela.neighborhood.impl.*;
import org.nextsus.cso.ela.sampling.Walk;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.DominanceComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

import java.util.ArrayList;
import java.util.List;

public class LocalFeatures {
    protected Problem<BinaryCSOSolution> problem;
    protected List<BinaryCSOSolution> solutionSet;
    protected DominanceComparator<BinaryCSOSolution> dominanceComparator;
    protected Class<? extends Walk> walk;
    protected Neighborhood n;

    public LocalFeatures(Problem<BinaryCSOSolution> problem, List<BinaryCSOSolution> solutionSet, Class<? extends Walk> walk, Neighborhood.NeighborhoodType neighborhoodType) {
        this.problem = problem;
        this.solutionSet = solutionSet;
        this.dominanceComparator = new DefaultDominanceComparator<>();
        this.walk = walk;
        this.n = switch (neighborhoodType) {
            case Tower -> new TowerNeighborhood(problem);
            case BS -> new BSNeighborhood(problem);
            case Sector -> new SectorNeighborhood(problem);
            case Cell -> new CellNeighborhood(problem);
            case Hamming -> new HammingNeighborhood(problem);
        };
    }

    public void execute() {
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

        System.out.println("\n# Computing local landscape features #\n");

        double x = 0.0;
        double y = ((StaticCSO) problem).getCapacityUpperLimit();
        PISAHypervolume hv = new PISAHypervolume(new double[]{x, y});

        List<Double> inf_avg_list = new ArrayList<>();
        List<Double> sup_avg_list = new ArrayList<>();
        List<Double> inc_avg_list = new ArrayList<>();
        List<Double> lnd_avg_list = new ArrayList<>();
        List<Double> lsupp_avg_list = new ArrayList<>();
        List<Double> hv_avg_list = new ArrayList<>();
        List<Double> hvd_avg_list = new ArrayList<>();
        List<Double> nhv_avg_list = new ArrayList<>();

        for (BinaryCSOSolution solution : solutionSet) {
//            List<BinaryCSOSolution> neighborhood = getHammingNeighborhood(solution);
            List<BinaryCSOSolution> neighborhood = n.getNeighborhood(solution);

            // To obtain the PS
            NonDominatedSolutionListArchive<BinaryCSOSolution> nonDominatedSolutionArchive = new NonDominatedSolutionListArchive<>();
            nonDominatedSolutionArchive.addAll(neighborhood);

            lnd_avg_list.add((double) nonDominatedSolutionArchive.size());
            lsupp_avg_list.add(getSupportedSolutions(nonDominatedSolutionArchive.solutions()));

            double[][] frontSolution = solutionListToDoubleMatrix(List.of(solution));
            double solutionHV = hv.compute(frontSolution);

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
                hv_list.add(hv.compute(frontNeighbor));
                hvd_list.add(Math.abs(solutionHV - hv_list.get(hv_list.size() - 1)));
            }
            inf_avg_list.add(inf);
            sup_avg_list.add(sup);
            inc_avg_list.add(inc);

            hv_avg_list.add(hv_list.stream().mapToDouble(i -> i).sum() / neighborhood.size());   // Promedio HV de los vecinos
            hvd_avg_list.add(hvd_list.stream().mapToDouble(i -> i).sum() / neighborhood.size()); // Diferencia promedio entre el HV de los vecinos y el de la solución actual
            nhv_avg_list.add(hv.compute(solutionListToDoubleMatrix(neighborhood)));              // HV del vecindario completo
        }

        int inf_avg = (int) inf_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        int sup_avg = (int) sup_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        int inc_avg = (int) inc_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        int lnd_avg = (int) lnd_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        int lsupp_avg = (int) lsupp_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double hv_avg = hv_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double hvd_avg = hvd_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
        double nhv_avg = nhv_avg_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();

        // Print common features between random and adaptive walk
        System.out.println("#inf_avg = " + inf_avg);
        System.out.println("#sup_avg = " + sup_avg);
        System.out.println("#inc_avg = " + inc_avg);
        System.out.println("#lnd_avg = " + lnd_avg);
        System.out.println("#lsupp_avg = " + lsupp_avg);
        System.out.println("hv_avg = " + hv_avg);
        System.out.println("hvd_avg = " + hvd_avg);
        System.out.println("nhv_avg = " + nhv_avg);

        if (walk.getSimpleName().equals("RandomWalk")) {
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
            List<Double> inf_r1_rws_list = inf_avg_list;
            inf_r1_rws_list.remove(0);
            double inf_r1_rws = computeCorrelation(inf_avg_list, inf_r1_rws_list);

            List<Double> sup_r1_rws_list = sup_avg_list;
            sup_r1_rws_list.remove(0);
            double sup_r1_rws = computeCorrelation(sup_avg_list, sup_r1_rws_list);

            List<Double> inc_r1_rws_list = inc_avg_list;
            inc_r1_rws_list.remove(0);
            double inc_r1_rws = computeCorrelation(inc_avg_list, inc_r1_rws_list);

            List<Double> lnd_r1_rws_list = lnd_avg_list;
            lnd_r1_rws_list.remove(0);
            double lnd_r1_rws = computeCorrelation(lnd_avg_list, lnd_r1_rws_list);

            List<Double> lsupp_r1_rws_list = lsupp_avg_list;
            lsupp_r1_rws_list.remove(0);
            double lsupp_r1_rws = computeCorrelation(lsupp_avg_list, lsupp_r1_rws_list);

            List<Double> hv_r1_rws_list = hv_avg_list;
            hv_r1_rws_list.remove(0);
            double hv_r1_rws = computeCorrelation(hv_avg_list, hv_r1_rws_list);

            List<Double> hvd_r1_rws_list = hvd_avg_list;
            hvd_r1_rws_list.remove(0);
            double hvd_r1_rws = computeCorrelation(hvd_avg_list, hvd_r1_rws_list);

            List<Double> nhv_r1_rws_list = nhv_avg_list;
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
        }
    }

    protected double computeCorrelation(List<Double> x, List<Double> y) {
        double mean_x = 0.0;
        double mean_y = 0.0;
        for (int i = 0; i < x.size(); i++) {
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
