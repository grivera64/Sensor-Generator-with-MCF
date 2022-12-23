/**
 * Represents a Sensor Node in a Network that has overflow data packets to store.
 *
 * @see SensorNode
 */
public class DataNode extends SensorNode {

    private static int idCounter = 1;

    public DataNode(double x, double y, double tr) {
        super(x, y, tr, String.format("DN%02d", idCounter++));
    }

    public static void resetCounter() {
        idCounter = 1;
    }
}
