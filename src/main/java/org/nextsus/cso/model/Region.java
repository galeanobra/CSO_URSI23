package org.nextsus.cso.model;

/**
 * @author paco
 */
public class Region {
    int id;
    int x;
    int y;

    //Propagation parameters
    //long seed_;
    double pathloss;
    double mean; //mean value for the random variable
    double std_db; //standard deviation for the random variable
    double kd;
    int channelType;

    /**
     * Parametrized constructor
     *
     * @param x Coordinate x of the region attraction point
     * @param y Coordinate y of the region attraction point
     */
    public Region(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;

        // Generates a pathloss between 2 and 3
        this.pathloss = 2.0 + UDN.random.nextDouble();
        // Generates a mean and std for the random variable
        this.mean = UDN.random.nextDouble();
        this.std_db = 0.5 + 2.0 * UDN.random.nextDouble();
        this.kd = 20.0 * UDN.random.nextDouble();
        this.channelType = 1 + UDN.random.nextInt(5);
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getPathloss() {
        return pathloss;
    }

    public double getMean() {
        return mean;
    }

    public double getStd_db() {
        return std_db;
    }

    public double getKd() {
        return kd;
    }

    public int getChannelType() {
        return channelType;
    }
}
