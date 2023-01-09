import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the basis of a Network with Data and Storage Nodes
 */
public interface Network {
    double getWidth();
    double getLength();
    List<SensorNode> getSensorNodes();
    List<SensorNode> getDataNodes();
    List<SensorNode> getStorageNodes();
    boolean isConnected();
    boolean isFeasible();
    Map<SensorNode, Set<SensorNode>> getAdjacencyList();    // Returns the connection of nodes (using ID)
    int calculateMinCost(SensorNode from, SensorNode to);
    List<SensorNode> getMinCostPath(SensorNode from, SensorNode to);
    int calculateCostOfPath(List<SensorNode> path);
    void save(String fileName);
    void saveAsCsInp(String fileName);
}
