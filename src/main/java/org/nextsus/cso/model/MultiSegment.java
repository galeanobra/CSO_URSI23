package org.nextsus.cso.model;

import org.nextsus.cso.model.cells.Cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing the interest points (boundaries of propagation regions) of a discrete line between a Cell and a Point
 */
public class MultiSegment implements Serializable {

    Point p;
    Cell c;

    public MultiSegment(Point p, Cell c) {
        this.p = p;
        this.c = c;
    }

    /**
     * Get the discrete line between a Cell and a Point, and returns the boundaries Points of the propagations regions that crosses the line
     *
     * @return List of boundaries Points
     */
    public List<Point> divideSegment() {
        UDN udn = p.udn;

        List<Point> line = new ArrayList<>();
        line.add(udn.getGridPoint(c.getSector().getX(), c.getSector().getY(), c.getSector().getZ()));
        Point auxPoint = line.get(0);

        while (!samePoint(line.get(line.size() - 1)) || !isNeighbor(auxPoint.x, auxPoint.y)) {
            int p_x = auxPoint.x;
            int p_y = auxPoint.y;

            List<Point> aux = new ArrayList<>();
            if (p.x > p_x && p.y > p_y) {
                if (p_x + 1 < udn.gridPointsX) aux.add(udn.getGridPoint(p_x + 1, p_y, 0));
                if (p_y + 1 < udn.gridPointsY) aux.add(udn.getGridPoint(p_x, p_y + 1, 0));
                if (p_x + 1 < udn.gridPointsX && p_y + 1 < udn.gridPointsY) aux.add(udn.getGridPoint(p_x + 1, p_y + 1, 0));
            } else if (p.x > p_x && p.y < p_y) {
                if (p_x + 1 < udn.gridPointsX) aux.add(udn.getGridPoint(p_x + 1, p_y, 0));
                if (p_y - 1 > 0) aux.add(udn.getGridPoint(p_x, p_y - 1, 0));
                if (p_x + 1 < udn.gridPointsX && p_y - 1 > 0) aux.add(udn.getGridPoint(p_x + 1, p_y - 1, 0));
            } else if (p.x < p_x && p.y > p_y) {
                if (p_x - 1 > 0) aux.add(udn.getGridPoint(p_x - 1, p_y, 0));
                if (p_y + 1 < udn.gridPointsY) aux.add(udn.getGridPoint(p_x, p_y + 1, 0));
                if (p_x - 1 > 0 && p_y + 1 < udn.gridPointsY) aux.add(udn.getGridPoint(p_x - 1, p_y + 1, 0));
            } else if (p.x < p_x && p.y < p_y) {
                if (p_x - 1 > 0) aux.add(udn.getGridPoint(p_x - 1, p_y, 0));
                if (p_y - 1 > 0) aux.add(udn.getGridPoint(p_x, p_y - 1, 0));
                if (p_x - 1 > 0 && p_y - 1 > 0) aux.add(udn.getGridPoint(p_x - 1, p_y - 1, 0));
            } else if (p.x == p_x) {
                if (p.y < p_y) aux.add(udn.getGridPoint(p_x, p_y - 1, 0));
                if (p.y > p_y) aux.add(udn.getGridPoint(p_x, p_y + 1, 0));
            } else {
                if (p.x < p_x) aux.add(udn.getGridPoint(p_x - 1, p_y, 0));
                if (p.x > p_x) aux.add(udn.getGridPoint(p_x + 1, p_y, 0));
            }

            Point closestPoint = getClosestPoint(aux);
            if (closestPoint.getPropagationRegion().getId() != auxPoint.getPropagationRegion().getId() && (closestPoint.getPoint2D()[0] != auxPoint.getPoint2D()[0] || closestPoint.getPoint2D()[1] != auxPoint.getPoint2D()[1]))
                line.add(auxPoint);

            if (isNeighbor(closestPoint.x, closestPoint.y)) {
                if (closestPoint.getPropagationRegion().getId() != p.getPropagationRegion().getId())
                    line.add(closestPoint);
                line.add(p);
            }

            auxPoint = closestPoint;
        }

        if (line.size() == 1) line.add(p);

        return line;
    }

    /**
     * Check if a Point is in the same 2D position than Point p
     *
     * @param aux Point to check
     * @return true if is the same 2D point
     */
    public boolean samePoint(Point aux) {
        return (aux.x == p.x && aux.y == p.y);
    }

    /**
     * Starting from a list of Points, returns the Point closest to Point p
     *
     * @param points List of candidate Points
     * @return Closest Point
     */
    public Point getClosestPoint(List<Point> points) {
        double min = Double.MAX_VALUE;
        Point minPoint = null;
        for (Point aux : points) {
            double distance = p.udn.distance(p.x, p.y, p.z, aux.x, aux.y, aux.z);
            if (distance < min) {
                min = distance;
                minPoint = aux;
            }
        }
        return minPoint;
    }

    /**
     * Check if a Point is neighbor of Point p using their [x, y] coordinates
     *
     * @param x x coordinate of the Point
     * @param y y coordinate of the Point
     * @return true if is neighbor of Point p
     */
    public boolean isNeighbor(int x, int y) {
        if (x == p.x && ((y - 1 >= 0 && y == p.y) || (y + 1 < p.udn.gridPointsY && y == p.y))) return true;
        else if (y == p.y && ((x - 1 >= 0 && x == p.x) || (x + 1 < p.udn.gridPointsX && x == p.x))) return true;
        else if (x - 1 >= 0 && y - 1 >= 0 && x - 1 == p.x && y - 1 == p.y) return true;
        else if (x - 1 >= 0 && y + 1 < p.udn.gridPointsY && x - 1 == p.x && y + 1 == p.y) return true;
        else if (x + 1 < p.udn.gridPointsX && y - 1 >= 0 && x + 1 == p.x && y - 1 == p.y) return true;
        else if (x + 1 < p.udn.gridPointsX && y + 1 < p.udn.gridPointsY && x + 1 == p.x && y + 1 == p.y) return true;

        return false;
    }
}