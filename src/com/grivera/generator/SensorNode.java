package com.grivera.generator;

/**
 * Represents the basic form of a Sensor Node in a Sensor com.grivera.generator.Network
 */
public abstract class SensorNode {

    private static int uuidCounter = 1;

    private static final int BITS_PER_PACKET = 3200;
    private static final double E_elec = 100e-9;
    private static final double E_amp = 100e-12;

    private int uuid;
    private final double x, y, tr;
    private String name;
    private int batteryCapacity;
    private int power;

    public SensorNode(double x, double y, double tr, String name, int c) {
        this.x = x;
        this.y = y;
        this.tr = tr;
        this.name = name;
        this.batteryCapacity = c;
        this.power = c;
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
    
    public void setBatteryCapacity(int batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
        this.reserPower();
    }
    
    public int getPower() {
        return this.power;
    }
    
    public boolean hasPower() {
        return this.power > 0;
    }
    
    public void resetPower() {
        this.power = this.batteryCapacity;
    }

    public int calculateTransmissionCost(SensorNode receiverNode, int packetsToSend) {
        int totalBits = BITS_PER_PACKET * packetsToSend;
        int cost = totalBits * (E_elec + E_amp * Math.pow(this.distanceTo(receiverNode), 2));
        return cost * Math.pow(10, -6);
    }

    public int calculateReceivingCost(int packetsToReceive) {
        int totalBits = BITS_PER_PACKET * packetsToReceive;
        int cost = totalBits * E_elec;
        return cost * Math.pow(10, -6);
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
