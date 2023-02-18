package com.grivera.generator;

/**
 * Represents a Sensor Node in a com.grivera.generator.Network that has storage space for overflow data packets.
 *
 * @see SensorNode
 */
public class StorageNode extends SensorNode {

    private static int idCounter = 1;
    private int capacity;
    private int usedSpace;

    public StorageNode(double x, double y, double tr, int capacity) {
        super(x, y, tr, String.format("SN%02d", idCounter++));
        this.setCapacity(capacity);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.usedSpace = 0;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getUsedSpace() {
        return this.usedSpace;
    }

    public boolean isFull() {
        return this.usedSpace >= this.capacity;
    }

    public boolean canStore(int deltaPackets) {
        return this.usedSpace + deltaPackets <= this.capacity;
    }

    public void storePackets(int packets) {
        if (!this.canStore(packets)) {
            throw new IllegalArgumentException(
                    String.format("%s cannot store %d packets (%d/%d full)",
                            this.getName(), packets, this.usedSpace, this.capacity
                    )
            );
        }
        this.usedSpace += packets;
    }

    @Override
    public void resetPackets() {
        this.usedSpace = 0;
    }

    public int getSpaceLeft() {
        return this.capacity - this.usedSpace;
    }

    public static void resetCounter() {
        idCounter = 1;
    }
}
