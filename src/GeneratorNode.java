public class GeneratorNode extends SensorNode {

    private static int id = 1;
    private final String name;

    public GeneratorNode(double x, double y) {
        super(x, y);
        this.name = String.format("DN%02d", id++);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeneratorNode gn)) {
            return false;
        }
        return this.getName().equals(gn.getName());
    }
}
