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

    public boolean inRangeOf(SensorNode o, double tr) {
        return Math.sqrt(Math.pow(this.x - o.x, 2) + Math.pow(this.y - o.y, 2)) <= tr + 0.0001;
    }


}
