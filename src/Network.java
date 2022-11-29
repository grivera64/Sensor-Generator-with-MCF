import java.util.List;

public interface Network {
    List<SensorNode> getSensorNodes();
    List<SensorNode> getGeneratorNodes();
    List<SensorNode> getStorageNodes();
    boolean isConnected();
}
