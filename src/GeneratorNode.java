public class GeneratorNode extends SensorNode {

    private static int idCounter = 1;

    public GeneratorNode(double x, double y, double tr) {
        super(x, y, tr, String.format("DN%02d", idCounter++));
    }

    public static void resetCounter() {
        idCounter = 1;
    }
}
