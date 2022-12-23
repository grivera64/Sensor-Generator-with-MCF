/**
 * Represents a Sensor Node in a Network that has storage space for overflow data packets.
 *
 * @see SensorNode
 */
public class StorageNode extends SensorNode {

    private static int idCounter = 1;

    public StorageNode(double x, double y, double tr) {
        super(x, y, tr, String.format("SN%02d", idCounter++));
    }

    public static void resetCounter() {
        idCounter = 1;
    }
}
