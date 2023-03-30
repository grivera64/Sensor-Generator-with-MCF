package com.grivera.generator.sensors;

/**
 * Represents the basic form of a Sensor Node in a Sensor com.grivera.generator.Network
 */
public abstract class SensorNode {

    private static int uuidCounter = 1;

    protected static final int BITS_PER_PACKET = 3200;
    protected static final double E_elec = 100e-9;
    protected static final double E_amp = 100e-12;

    private int uuid;
    private final double x, y, tr;
    private String name;
    private int batteryCapacity;
    private int energy;

    public SensorNode(double x, double y, double tr, String name, int c) {
        this.x = x;
        this.y = y;
        this.tr = tr;
        this.name = name;
        this.batteryCapacity = c;
        this.energy = c;
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
        this.resetEnergy();
    }
    
    public int getEnergy() {
        return this.energy;
    }
    
    public boolean hasEnergy() {
        return this.energy > 0;
    }
    
    public void resetEnergy() {
        this.energy = this.batteryCapacity;
    }

    public boolean canTransmit(SensorNode receiverNode) {
        return this.calculateTransmissionCost(receiverNode) <= this.energy;
    }

    /**
     * Calculates the cost to transmit/relay a data packet from this Sensor Node to a specified receiver Sensor Node
     *
     * @param receiverNode the Sensor Node that this Sensor Node would transmit/relay one data packet to
     * @return the cost to transmit/relay one data packet from this Sensor Node to the receiver Sensor Node
     */
    public int calculateTransmissionCost(SensorNode receiverNode) {
        double cost = BITS_PER_PACKET * (E_elec + E_amp * Math.pow(this.distanceTo(receiverNode), 2));
        return (int) Math.round(cost * Math.pow(10, 6));
    }

    public boolean canReceive() {
        return this.calculateReceivingCost() <= this.energy;
    }

    /**
     * Calculates the cost for this Sensor Node to receive one transmitted/relayed data packet
     *
     * @return the cost for this Sensor Node to receive a transmitted/relayed data packet
     */
    public int calculateReceivingCost() {
        double cost = BITS_PER_PACKET * E_elec;
        return (int) Math.round(cost * Math.pow(10, 6));
    }

    public abstract int calculateStorageCost();

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
