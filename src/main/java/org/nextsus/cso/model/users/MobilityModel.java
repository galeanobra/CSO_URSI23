package org.nextsus.cso.model.users;

/**
 * Mobility models for the dynamic problem
 */
public abstract class MobilityModel {

    abstract public void move(User u);

    public abstract double getMinV_();

    public abstract double getMaxV_();
}
