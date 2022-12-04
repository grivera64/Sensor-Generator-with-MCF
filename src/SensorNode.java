public abstract class SensorNode {

    private static int uuidCounter = 1;
    private int uuid;
    private final double x, y;

    public SensorNode(double x, double y) {
        this.x = x;
        this.y = y;
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

    public double distanceTo(SensorNode o) {
        return Math.sqrt(Math.pow(this.x - o.x, 2) + Math.pow(this.y - o.y, 2));
    }

    public boolean inRangeOf(SensorNode o, double tr) {
        return this.distanceTo(o) <= tr + 0.0001;
    }

    @Override
    public String toString() {
        return String.format("%-14s(%.6f, %.6f) [Node %d]", this.getName(), this.getX(), this.getY(), this.getUuid());
    }

    public abstract String getName();

}
