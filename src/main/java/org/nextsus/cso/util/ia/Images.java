package org.nextsus.cso.util.ia;

import org.nextsus.cso.model.UDN;
import org.nextsus.cso.model.cells.Cell;
import org.nextsus.cso.model.users.User;
import org.nextsus.cso.problem.StaticCSO;
import org.nextsus.cso.solution.BinaryCSOSolution;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.binarysolution.impl.DefaultBinarySolution;
import org.uma.jmetal.util.binarySet.BinarySet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Images {
    // Argumentos de entrada:
    //      [Nombre del fichero] [nombre del main] [semilla]
    // Lee el fichero:
    //      00101

    // Despliega y calcula la SINR para cada tipo de celda y para cada punto
    // Guarda n ficheros, tantos como tipos de celdas, y cada uno con su grid
    // Guarda un fichero con el despliegue en el grid (1, hay SBS; 0, no)

    public static void main(String[] args) throws IOException {
        File folder = new File(args[0]);
        String scenario = args[1];
        int run = Integer.parseInt(args[2]);

        File[] listOfFilesFUN = folder.listFiles((dir, name) -> name.startsWith("FUN"));
        File[] listOfFilesVAR = folder.listFiles((dir, name) -> name.startsWith("VAR"));

        if (listOfFilesFUN == null || listOfFilesVAR == null) {
            System.out.println("Empty or wrong directory");
            System.exit(-1);
        }

        StaticCSO cso = new StaticCSO("main.properties", scenario, run);

        int counter = 0;

        for (int i = 0; i < listOfFilesFUN.length; i++) {

            File fileVAR = listOfFilesVAR[i];
            File fileFUN = listOfFilesFUN[i];

            BufferedReader brVAR = new BufferedReader(new FileReader(fileVAR));
            BufferedReader brFUN = new BufferedReader(new FileReader(fileFUN));

            List<String> VAR = brVAR.lines().toList();
            List<String> FUN = brFUN.lines().toList();

            for (int j = 0; j < FUN.size(); j++) {

                counter = (i == 0) ? j : FUN.size() * i + j;

                System.out.println(counter);

                String solutionString = VAR.get(j);
                boolean[] solutionBool = new boolean[solutionString.length()];

                for (int k = 0; k < solutionString.length(); k++) {
                    solutionBool[k] = solutionString.charAt(k) != '0';
                }

                BinarySet bits = new BinarySet(cso.getTotalNumberOfActivableCells());
                for (int k = 0; k < bits.getBinarySetLength(); k++) {
                    bits.set(k, solutionBool[k]);
                }
                BinaryCSOSolution s = new BinaryCSOSolution(bits);

                cso.getUDN().setCellActivation(s.variables().get(0));
                cso.evaluate(s);

                List<UDN.CellType> activeTypes = new ArrayList<>();

                for (UDN.CellType type : UDN.CellType.values()) {
                    if (cso.getUDN().getNumberOfActiveCellsByType(type) > 0) {
                        activeTypes.add(type);
                    }
                }

                for (UDN.CellType type : activeTypes) {
                    FileWriter fw = new FileWriter("data/IA/raw/snr/" + counter + "_" + type.toString().toLowerCase());
                    PrintWriter pw = new PrintWriter(fw);

                    for (int k = 0; k < cso.getUDN().gridPointsX; k++) {
                        for (int l = 0; l < cso.getUDN().gridPointsY; l++) {
                            pw.print(cso.getUDN().getGridPoint(k, l, 0).computeSNR(cso.getUDN().getGridPoint(k, l, 0).getCellWithHigherSNRByType(type)));
                            pw.print(" ");
                        }
                        pw.print("\n");
                    }
                    pw.close();
                    fw.close();
                }

                FileWriter fw = new FileWriter("data/IA/raw/snr/" + counter + "_max");
                PrintWriter pw = new PrintWriter(fw);

                for (int k = 0; k < cso.getUDN().gridPointsX; k++) {
                    for (int l = 0; l < cso.getUDN().gridPointsY; l++) {
                        pw.print(cso.getUDN().getGridPoint(k, l, 0).computeSNR(cso.getUDN().getGridPoint(k, l, 0).getCellWithHigherSNR()));
                        pw.print(" ");
                    }
                    pw.print("\n");
                }
                pw.close();
                fw.close();

                for (UDN.CellType type : activeTypes) {
                    FileWriter fwDeployment = new FileWriter("data/IA/raw/deployment/" + counter + "_" + type.toString().toLowerCase());
                    PrintWriter pwDeployment = new PrintWriter(fwDeployment);
                    for (int k = 0; k < cso.getUDN().gridPointsX; k++) {
                        for (int l = 0; l < cso.getUDN().gridPointsY; l++) {
                            boolean deployed = false;
                            boolean active = false;
                            for (int m = 0; m < 3; m++) {
                                if (cso.getUDN().getGridPoint(k, l, m).hasBTSInstalled()) {
                                    for (double d : cso.getUDN().getGridPoint(k, l, m).getInstalledBTS().keySet()) {
                                        for (Cell c : cso.getUDN().getGridPoint(k, l, m).getInstalledBTS().get(d).getPoint().getCells()) {
                                            if (c.getType().equals(type)) {
                                                deployed = true;
                                                if (c.isActive()) {
                                                    active = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (deployed && active) {
                                pwDeployment.print(2);
                            } else if (deployed) {
                                pwDeployment.print(1);
                            } else {
                                pwDeployment.print(0);
                            }
                            pwDeployment.print(" ");
                        }
                        pwDeployment.print("\n");
                    }
                    pwDeployment.close();
                    fwDeployment.close();
                }


                // Objectives
                fw = new FileWriter("data/IA/raw/objectives/" + counter + ".csv");
                pw = new PrintWriter(fw);
                pw.print(FUN.get(j));
                pw.print(" ");
                pw.close();
                fw.close();

                if (counter == 0) {
                    // Usuarios
                    FileWriter fwUE = new FileWriter("data/IA/raw/users");
                    PrintWriter pwUE = new PrintWriter(fwUE);
                    for (int k = 0; k < cso.getUDN().gridPointsX; k++) {
                        for (int l = 0; l < cso.getUDN().gridPointsY; l++) {
                            boolean user = false;
                            for (User u : cso.getUDN().getUsers()) {
                                if (u.getX() == k && u.getY() == l) {
                                    user = true;
                                    break;
                                }
                            }

                            if (user) {
                                pwUE.print(1);
                            } else {
                                pwUE.print(0);
                            }
                            pwUE.print(" ");
                        }
                        pwUE.print("\n");
                    }
                    pwUE.close();
                    fwUE.close();

                    // PathLoss
                    fw = new FileWriter("data/IA/raw/pathloss");
                    pw = new PrintWriter(fw);

                    for (int k = 0; k < cso.getUDN().gridPointsX; k++) {
                        for (int l = 0; l < cso.getUDN().gridPointsY; l++) {
                            pw.print(cso.getUDN().getGridPoint(k, l, 0).getPropagationRegion().getPathloss());
                            pw.print(" ");
                        }
                        pw.print("\n");
                    }
                    pw.close();
                    fw.close();
                }
            }
        }
    }
}
