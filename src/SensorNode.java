public abstract class SensorNode {
    private final double x, y;

    public SensorNode(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public boolean isConnectedTo(SensorNode o, double tr) {
        return Math.sqrt(Math.pow(this.x - o.x, 2) + Math.pow(this.y - o.y, 2)) <= tr + 0.0001;
    }
}
