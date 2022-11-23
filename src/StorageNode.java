public class StorageNode extends SensorNode {

    private static int id = 1;
    private final String name;

    public StorageNode(double x, double y) {
        super(x, y);
        this.name = String.format("SN%2d", id++);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StorageNode sn)) {
            return false;
        }
        return this.getName().equals(sn.getName());
    }

    @Override
    public String toString() {
        return String.format("%10s(%.6f, %.6f)", this.getName(), this.getX(), this.getY());
    }
}
