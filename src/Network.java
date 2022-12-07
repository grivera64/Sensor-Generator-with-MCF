import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Network {
    double getWidth();
    double getLength();
    List<SensorNode> getSensorNodes();
    List<SensorNode> getGeneratorNodes();
    List<SensorNode> getStorageNodes();
    boolean isConnected();
    boolean isFeasible();
    Map<SensorNode, Set<SensorNode>> getAdjacencyLists();    // Returns the connection of nodes (using ID)
    List<SensorNode> getMinCostPath(SensorNode from, SensorNode to);
    void saveAsCsInp(String fileName);
}
