package com.grivera.generator;

/**
 * Represents the basic form of a Sensor Node in a Sensor com.grivera.generator.Network
 */
public abstract class SensorNode {

    private static int uuidCounter = 1;

    private int uuid;
    private final double x, y, tr;
    private String name;

    public SensorNode(double x, double y, double tr, String name) {
        this.x = x;
        this.y = y;
        this.tr = tr;
        this.name = name;
        this.setUuid();
    }

    private void setUuid() {
        this.uuid = uuidCounter;
        uuidCounter++;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public int getUuid() {
        return this.uuid;
    }

    /**
     * Tests if this Sensor Node is within range of the specified Sensor Node
     *
     * @param o the specified Sensor Node
     * @return true if and only if this Sensor Node is within transmission range
     * of the specified Sensor Node; otherwise false
     */
    public boolean inRangeOf(SensorNode o) {
        return this.distanceTo(o) <= Math.min(this.tr, o.tr) + 0.0001;
    }

    /**
     * Calculates the distance between this Sensor Node and the specified Sensor Node
     *
     * @param o the specified Sensor Node
     * @return the distance between the two nodes (in meters)
     */
    public double distanceTo(SensorNode o) {
        return Math.sqrt(Math.pow(this.x - o.x, 2) + Math.pow(this.y - o.y, 2));
    }

    @Override
    public String toString() {
        return String.format("%-14s(%.6f, %.6f) [%d]", this.getName(), this.getX(), this.getY(), this.getUuid());
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SensorNode sn)) {
            return false;
        }
        return this.getUuid() == sn.getUuid();
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    public static void resetCounter() {
        uuidCounter = 1;
    }

    public abstract void resetPackets();
}
