package org.nextsus.cso.model.users;

import java.io.Serializable;

/**
 * Mobility models for the dynamic problem
 */
public abstract class MobilityModel implements Serializable {

    abstract public void move(User u);

    public abstract double getMinV_();

    public abstract double getMaxV_();
}
