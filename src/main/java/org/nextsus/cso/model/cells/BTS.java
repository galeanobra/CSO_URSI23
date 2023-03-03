/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nextsus.cso.model.cells;

import org.nextsus.cso.model.Point;
import org.nextsus.cso.model.UDN;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Source;
import us.hebi.matlab.mat.types.Sources;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Class representing a Base Transmission Station
 */
public class BTS {

    protected static int uniqueId_ = 0;
    static boolean patternFileLoaded_ = false;
    Matrix antennaArray_;
    int id_;

    // BTS location
    int x_;
    int y_;
    int z_;
    Point point;

    // Sectors
    List<Sector> sectors_;
    double workingFrequency_;

    UDN udn_;
    String name;
    UDN.CellType type_;


    public BTS(int x, int y, int z, String typeName, double workingFrequency, String radiationPatternFile, UDN udn, String name) {
        this.id_ = uniqueId_;
        uniqueId_++;

        this.x_ = x;
        this.y_ = y;
        this.z_ = z;
        this.workingFrequency_ = workingFrequency;
        this.udn_ = udn;
        this.point = udn_.getGridPoint(x_, y_, z_);
        this.name = name;
        this.sectors_ = new LinkedList<>();

        // Assign BTS type
        if (typeName.equalsIgnoreCase("femto")) {
            this.type_ = UDN.CellType.FEMTO;
        } else if (typeName.equalsIgnoreCase("pico")) {
            this.type_ = UDN.CellType.PICO;
        } else if (typeName.equalsIgnoreCase("micro")) {
            this.type_ = UDN.CellType.MICRO;
        } else if (typeName.equalsIgnoreCase("macro")) {
            this.type_ = UDN.CellType.MACRO;
        }
        try {
            InputStream in = getClass().getResourceAsStream("/common/" + radiationPatternFile);
            Source source = Sources.wrapInputStream(Objects.requireNonNull(in));
            antennaArray_ = Mat5.newReader(source).readMat().getMatrix(0);
        } catch (Exception e) {
            System.out.println("Error loading MAT file " + "/common/" + radiationPatternFile);
            System.exit(-1);
        }
        patternFileLoaded_ = true;
    }

    public BTS(BTS b) {
        this.id_ = b.id_;
        this.x_ = b.x_;
        this.y_ = b.y_;
        this.z_ = b.z_;
        this.workingFrequency_ = b.workingFrequency_;
        this.point = b.point;
        this.udn_ = b.udn_;
        this.name = b.name;
        this.sectors_ = b.sectors_;
        this.type_ = b.type_;
        this.antennaArray_ = b.antennaArray_;
    }

    public void addSectors(String type, String name, int numChainsTX, int numSectors, double transmittedPower, double alfa, double beta, double delta, double transmitterGain, double receptorGain, double workingFrequency, double coverageRadius) {
        for (int i = 0; i < numSectors; i++) {
            sectors_.add(new Sector(x_, y_, z_, udn_, this, type, name, numChainsTX, transmittedPower, alfa, beta, delta, transmitterGain, receptorGain, workingFrequency, coverageRadius));
        }
    }

    public int getNumberOfActiveCells() {
        int num = 0;
        for (Sector sector : this.sectors_) {
            for (Cell cell : sector.cells_) {
                if (cell.active_) {
                    num++;
                }
            }
        }
        return num;
    }

    public int getNumberOfInstalledCells() {
        int num = 0;
        for (Sector sector : this.sectors_) {
            for (Cell cell : sector.cells_) {
                num++;
            }
        }
        return num;
    }

    public List<Sector> getActiveSectors() {
        return sectors_.stream().filter(s -> s.getActiveCells() > 0).toList();
    }

    public int getId() {
        return id_;
    }

    public int getX() {
        return x_;
    }

    public int getY() {
        return y_;
    }

    public int getZ() {
        return z_;
    }

    public List<Sector> getSectors() {
        return sectors_;
    }

    public double getWorkingFrequency() {
        return workingFrequency_;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    @Override
    public String toString() {
        return "BTS(" + x_ + "," + y_ + ')';
    }
}
