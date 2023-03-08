/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nextsus.cso.model;

import org.nextsus.cso.model.cells.BTS;
import org.nextsus.cso.model.users.User;
import org.nextsus.cso.util.PPP;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * @author paco
 */
public class StaticUDN extends UDN implements Serializable {

    /**
     * Default constructor
     *
     * @param mainConfigFile
     * @param run
     */
    public StaticUDN(String mainConfigFile, String scenario, int run) {
        super(mainConfigFile, scenario, run);

        //load Users info
        loadUsers(staticUserConfigFile, scenario);

        //load hethetnet config
        loadHetHetNetConfig(hetNetConfigFile);
    }

    /**
     * Load the config file with the different type of users
     *
     * @param propFile The filename of the configuration file
     */
    private void loadUsers(String propFile, String scenario) {

        System.out.println("Loading users config file...");

        Properties pro = new Properties();
        try (InputStream resourceStream = getClass().getResourceAsStream("/common/" + propFile)) {
            pro.load(resourceStream);
        } catch (IOException e) {
            System.out.println("Error loading properties " + propFile);
            System.exit(-1);
        }

        this.usersTypes = Integer.parseInt(pro.getProperty("numUserTypes"));
        this.usersConfig = new ArrayList<>();
        for (int i = 0; i < this.usersTypes; i++) {
            this.usersConfig.add("/scenarios/" + scenario + "/" + pro.getProperty("userType" + i));
        }

        this.users = new ArrayList<>();
        loadUserConfig(this.users, this.usersConfig);
    }

    /**
     * Load the configuration for each particular type of user
     *
     * @param users   The data structure to store the users
     * @param configs The filename of the configuration file
     */
    private void loadUserConfig(List<User> users, List<String> configs) {
        //variables
        Properties pro = new Properties();
        PPP ppp = new PPP(DynamicUDN.random);

        int numFemtoAntennas;
        int numPicoAntennas;
        int numMicroAntennas;
        int numMacroAntennas;
        int id = 0;
        double bw;
        String typename;

        for (String s : configs) {
            System.out.println("Loading user config file " + s);
            try (InputStream resourceStream = getClass().getResourceAsStream(s)) {
                pro.load(resourceStream);
            } catch (IOException e) {
                System.out.println("Error loading properties " + s);
                System.exit(-1);
            }

            //loading parameters
            bw = Double.parseDouble(pro.getProperty("trafficDemand", "1"));
            typename = pro.getProperty("userTypename");
            numFemtoAntennas = Integer.parseInt(pro.getProperty("numFemtoAntennas", "4"));
            numPicoAntennas = Integer.parseInt(pro.getProperty("numPicoAntennas", "4"));
            numMicroAntennas = Integer.parseInt(pro.getProperty("numMicroAntennas", "4"));
            numMacroAntennas = Integer.parseInt(pro.getProperty("numMacroAntennas", "4"));

            //load the number of cells of with this configuration
            int numUsers = Integer.parseInt(pro.getProperty("numUsers", "10"));
            //int numUsers = 15;
            double lambda = Double.parseDouble(pro.getProperty("lambdaForPPP", "50"));
            double mu = this.gridPointsY * this.gridPointsX * this.interPointSeparation * this.interPointSeparation;
            mu = mu / (1000000.0);
            //uncomment for PPP distributions
            numUsers = ppp.getPoisson(lambda * mu);
            System.out.println("\tNumber of users: " + numUsers);

            int deployedUsers = 0;
            while (deployedUsers < numUsers) {
                //randomize the position
                int x = random.nextInt(gridPointsX);
                int y = random.nextInt(gridPointsY);
                //check if there is a BTS installed in any of the points
                boolean hasBTS = false;
                for (int z = 0; z < this.gridPointsZ; z++) {
                    if (grid[x][y][z].hasBTSInstalled()) {
                        hasBTS = true;
                        break;
                    }
                }

                if (!hasBTS) {
                    //int z = random_.nextInt(gridPointsZ_);
                    int z = 0; //We assume all users are on the street

                    User user = new User(id, x, y, z, bw, typename, true, numFemtoAntennas, numPicoAntennas, numMicroAntennas, numMacroAntennas);

                    users.add(user);
                    deployedUsers++;
                    //update id
                    id++;
                }
            }
        }
    }

    /**
     * Load the main parameters of the instance
     */
    private void generateSocialAttractors() {

        System.out.println("Generating Social Attractors...");

        if ((this.users == null) || (this.users.size() == 0)) {
            System.out.println("Error generating social attractors. Missing users info.");
        }
        int numberOfSAs = this.users.size() / 10;
        System.out.println("\tNumber of SAs: " + numberOfSAs);

        socialAttractors = new ArrayList<>();
        for (int sa = 0; sa < numberOfSAs; sa++) {
            //randomize the position
            int x = random.nextInt(gridPointsX);
            int y = random.nextInt(gridPointsY);
            int z = random.nextInt(gridPointsZ);
            socialAttractors.add(new SocialAttractor(sa, x, y, z));
        }
    }


    /**
     * Load the config for using the procedure in M. Mirahsan, R. Schoenen, and H. Yanikomeroglu,
     * “HetHetNets: Heterogeneous Traffic Distribution in Heterogeneous Wireless Cellular Networks,”
     * IEEE J. Sel. Areas Commun., vol. 33, no. 10, pp. 2252–2265, 2015.
     *
     * @param propFile The configuration parameters
     */
    private void loadHetHetNetConfig(String propFile) {

        System.out.println("Loading deployment config file...");

        Properties pro = new Properties();
        try (InputStream resourceStream = getClass().getResourceAsStream("/common/" + propFile)) {
            pro.load(resourceStream);
        } catch (IOException e) {
            System.out.println("Error loading properties " + propFile);
            System.exit(-1);
        }

        this.alphaHetHetNet = Double.parseDouble(pro.getProperty("alpha", "0.1"));
        this.meanBetaHetHetNet = Double.parseDouble(pro.getProperty("meanBeta", "0.1"));

        generateSocialAttractors();

        // Move SA and UEs
        hetHetNet();
    }

    /**
     * Deploy BTSs and users following the procedure in M. Mirahsan, R. Schoenen, and H.
     * Yanikomeroglu, “HetHetNets: Heterogeneous Traffic Distribution in Heterogeneous Wireless
     * Cellular Networks,” IEEE J. Sel. Areas Commun., vol. 33, no. 10, pp. 2252–2265, 2015.
     */
    public void hetHetNet() {
        // Move every SA towards its closest BTS in terms of the received signal power, by a factor of alpha
        for (SocialAttractor sa : this.socialAttractors) {
            Point p = getGridPoint(sa.getX(), sa.getY(), sa.getZ());
            BTS b = p.getCellWithHigherReceivingPower().getBTS();
            sa.moveSATowardsBTS(b, this.alphaHetHetNet);
        }

        // Move every user towards its closest SA in terms of the Euclidean distance, by a factor of beta
        for (User u : this.users) {
            Point p = getGridPoint(u.getX(), u.getY(), u.getZ());
            SocialAttractor sa = p.getClosestSA(this);
            u.moveUserTowardsSA(sa, this, this.meanBetaHetHetNet);
        }
    }
}
