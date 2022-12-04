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


    public boolean inRangeOf(SensorNode o) {
        return this.distanceTo(o) <= Math.min(this.tr, o.tr) + 0.0001;
    }

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
        if (!(o instanceof StorageNode sn)) {
            return false;
        }
        return this.getUuid() == sn.getUuid();
    }

    public static void resetCounter() {
        uuidCounter = 1;
    }
}
