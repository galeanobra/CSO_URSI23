package org.nextsus.cso.ela.features;

import org.nextsus.cso.ela.sampling.Walk;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.problem.Problem;
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

    public LocalFeatures(Problem<BinaryCSOSolution> problem, List<BinaryCSOSolution> solutionSet, Class<? extends Walk> walk) {
        this.problem = problem;
        this.solutionSet = solutionSet;
        this.dominanceComparator = new DefaultDominanceComparator<>();
        this.walk = walk;
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

        if (walk.getSimpleName().equals("RandomWalk")) {
            List<Double> inf_avg_rws_list = new ArrayList<>();
            List<Double> sup_avg_rws_list = new ArrayList<>();
            List<Double> inc_avg_rws_list = new ArrayList<>();
            List<Double> lnd_avg_rws_list = new ArrayList<>();
            List<Double> lsupp_avg_rws_list = new ArrayList<>();
            List<Double> hv_avg_rws_list = new ArrayList<>();
            List<Double> hvd_avg_rws_list = new ArrayList<>();
            List<Double> nhv_avg_rws_list = new ArrayList<>();

            for (BinaryCSOSolution solution : solutionSet) {
                List<BinaryCSOSolution> neighborhod = getNeighborhood(solution);

                // To obtain the PS
                NonDominatedSolutionListArchive<BinaryCSOSolution> nonDominatedSolutionArchive = new NonDominatedSolutionListArchive<>();
                nonDominatedSolutionArchive.addAll(neighborhod);

                lnd_avg_rws_list.add((double) nonDominatedSolutionArchive.size());
                lsupp_avg_rws_list.add(getSupportedSolutions(nonDominatedSolutionArchive.solutions()));
                hv_avg_rws_list.add(0.0);
                hvd_avg_rws_list.add(0.0);
                nhv_avg_rws_list.add(0.0);

                double sup = 0.0;
                double inf = 0.0;
                double inc = 0.0;

                for (BinaryCSOSolution neighbor : neighborhod) {
                    switch (dominanceComparator.compare(neighbor, solution)) {
                        case -1 -> sup++;
                        case 1 -> inf++;
                        default -> inc++;
                    }

                    // TODO habría que definir un punto de referencia
                    // Calcular HV promedio de cada vecino
                    // Diferencia entre el HV del vecino y la solución actual
                    // HV cubierto por el vecindario completo TODO ?¿
                }
                inf_avg_rws_list.add(inf);
                sup_avg_rws_list.add(sup);
                inc_avg_rws_list.add(inc);
            }

            int inf_avg_aws = (int) inf_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            int sup_avg_aws = (int) sup_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            int inc_avg_aws = (int) inc_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            int lnd_avg_aws = (int) lnd_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            int lsupp_avg_aws = (int) lsupp_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double hv_avg_aws = hv_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double hvd_avg_aws = hvd_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double nhv_awd_aws = nhv_avg_rws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();

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
            List<Double> inf_r1_rws_list = inf_avg_rws_list;
            List<Double> sup_r1_rws_list = sup_avg_rws_list;
            List<Double> inc_r1_rws_list = inc_avg_rws_list;
            List<Double> lnd_r1_rws_list = lnd_avg_rws_list;
            List<Double> lsupp_r1_rws_list = lsupp_avg_rws_list;
            List<Double> hv_r1_rws_list = hv_avg_rws_list;
            List<Double> hvd_r1_rws_list = hvd_avg_rws_list;
            List<Double> nhv_r1_rws_list = nhv_avg_rws_list;

            inf_r1_rws_list.remove(0);
            sup_r1_rws_list.remove(0);
            inc_r1_rws_list.remove(0);
            lnd_r1_rws_list.remove(0);
            lsupp_r1_rws_list.remove(0);
            hv_r1_rws_list.remove(0);
            hvd_r1_rws_list.remove(0);
            nhv_r1_rws_list.remove(0);

            double inf_r1_aws = computeCorrelation(inf_avg_rws_list, inf_r1_rws_list);
            double sup_r1_aws = computeCorrelation(sup_avg_rws_list, sup_r1_rws_list);
            double inc_r1_aws = computeCorrelation(inc_avg_rws_list, inc_r1_rws_list);
            double lnd_r1_aws = computeCorrelation(lnd_avg_rws_list, lnd_r1_rws_list);
            double lsupp_r1_aws = computeCorrelation(lsupp_avg_rws_list, lsupp_r1_rws_list);
            double hv_r1_aws = computeCorrelation(hv_avg_rws_list, hv_r1_rws_list);
            double hvd_r1_aws = computeCorrelation(hvd_avg_rws_list, hvd_r1_rws_list);
            double nhv_r1_aws = computeCorrelation(nhv_avg_rws_list, nhv_r1_rws_list);

            // Print features
            System.out.println("#inf_avg_rws = " + inf_avg_aws);
            System.out.println("#inf_r1_rws = " + inf_r1_aws);
            System.out.println("#sup_avg_rws = " + sup_avg_aws);
            System.out.println("#sup_r1_rws = " + sup_r1_aws);
            System.out.println("#inc_avg_rws = " + inc_avg_aws);
            System.out.println("#inc_r1_rws = " + inc_r1_aws);
            System.out.println("#lnd_avg_rws = " + lnd_avg_aws);
            System.out.println("#lnd_r1_rws = " + lnd_r1_aws);
            System.out.println("#lsupp_avg_rws = " + lsupp_avg_aws);
            System.out.println("#lsupp_r1_rws = " + lsupp_r1_aws);
            System.out.println("hv_avg_rws = " + hv_avg_aws);
            System.out.println("hv_r1_rws = " + hv_r1_aws);
            System.out.println("hvd_avg_rws = " + hvd_avg_aws);
            System.out.println("hvd_r1_rws = " + hvd_r1_aws);
            System.out.println("nhv_awd_rws = " + nhv_awd_aws);
            System.out.println("nhv_r1_rws = " + nhv_r1_aws);
            System.out.println("f_cor_rws = " + f_cor_rws);
        } else if (walk.getSimpleName().equals("AdaptiveWalk")) {
            int length_aws = solutionSet.size() - 1;
            List<Integer> inf_avg_aws_list = new ArrayList<>();
            List<Integer> sup_avg_aws_list = new ArrayList<>();
            List<Integer> inc_avg_aws_list = new ArrayList<>();
            List<Integer> lnd_avg_aws_list = new ArrayList<>();
            List<Integer> lsupp_avg_aws_list = new ArrayList<>();
            List<Double> hv_avg_aws_list = new ArrayList<>();
            List<Double> hvd_avg_aws_list = new ArrayList<>();
            List<Double> nhv_awd_aws_list = new ArrayList<>();

            for (BinaryCSOSolution solution : solutionSet) {
                List<BinaryCSOSolution> neighborhod = getNeighborhood(solution);

                // To obtain the PS
                NonDominatedSolutionListArchive<BinaryCSOSolution> nonDominatedSolutionArchive = new NonDominatedSolutionListArchive<>();
                nonDominatedSolutionArchive.addAll(neighborhod);

                lnd_avg_aws_list.add(nonDominatedSolutionArchive.size());
                lsupp_avg_aws_list.add(0); //TODO
                hv_avg_aws_list.add(0.0);
                hvd_avg_aws_list.add(0.0);
                nhv_awd_aws_list.add(0.0);

                int sup = 0;
                int inf = 0;
                int inc = 0;

                for (BinaryCSOSolution neighbor : neighborhod) {
                    switch (dominanceComparator.compare(neighbor, solution)) {
                        case -1 -> sup++;
                        case 1 -> inf++;
                        default -> inc++;
                    }
                    // TODO habría que definir un punto de referencia
                    // Calcular HV promedio de cada vecino
                    // Diferencia entre el HV del vecino y la solución actual
                    // HV cubierto por el vecindario completo TODO ?¿
                }
                inf_avg_aws_list.add(inf);
                sup_avg_aws_list.add(sup);
                inc_avg_aws_list.add(inc);
            }

            int inf_avg_aws = inf_avg_aws_list.stream().mapToInt(i -> i).sum() / solutionSet.size();
            int sup_avg_aws = sup_avg_aws_list.stream().mapToInt(i -> i).sum() / solutionSet.size();
            int inc_avg_aws = inc_avg_aws_list.stream().mapToInt(i -> i).sum() / solutionSet.size();
            int lnd_avg_aws = lnd_avg_aws_list.stream().mapToInt(i -> i).sum() / solutionSet.size();
            int lsupp_avg_aws = lsupp_avg_aws_list.stream().mapToInt(i -> i).sum() / solutionSet.size();
            double hv_avg_aws = hv_avg_aws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double hvd_avg_aws = hvd_avg_aws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();
            double nhv_awd_aws = nhv_awd_aws_list.stream().mapToDouble(i -> i).sum() / solutionSet.size();

            // Print features
            System.out.println("#inf_avg_aws = " + inf_avg_aws);
            System.out.println("#sup_avg_aws = " + sup_avg_aws);
            System.out.println("#inc_avg_aws = " + inc_avg_aws);
            System.out.println("#lnd_avg_aws = " + lnd_avg_aws);
            System.out.println("#lsupp_avg_aws = " + lsupp_avg_aws);
            System.out.println("hv_avg_aws = " + hv_avg_aws);
            System.out.println("hvd_avg_aws = " + hvd_avg_aws);
            System.out.println("nhv_awd_aws = " + nhv_awd_aws);
            System.out.println("length_aws = " + length_aws);
        }
    }

    protected List<BinaryCSOSolution> getNeighborhood(BinaryCSOSolution solution) {
        List<BinaryCSOSolution> neighbors = new ArrayList<>();

        for (int i = 0; i < solution.getNumberOfBits(0); i++) {
            BinaryCSOSolution s = solution.copy();
            s.variables().get(0).flip(i);
            problem.evaluate(s);
            neighbors.add(s);
        }

        return neighbors;
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
}
