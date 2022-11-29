import java.util.*;

public class SensorNetwork implements Network {

    private List<SensorNode> nodes;
    private Map<SensorNode, Set<SensorNode>> graph;

    private final double width, length;
    private final int dataPacketCount;
    private final int storageCapacity;

    public SensorNetwork(double x, double y, int N, double tr, int p, int q, int m) {
        this.width = x;
        this.length = y;
        this.dataPacketCount = q;
        this.storageCapacity = m;

        this.nodes = this.initNodes(N, p);
        this.graph = this.initGraph(this.nodes, tr);
    }

    private List<SensorNode> initNodes(int nodeCount, int p) {
        List<SensorNode> nodes = new ArrayList<>(nodeCount);
        Random rand = new Random();

        int choice;
        double x, y;
        for (int index = 0; index < nodeCount; index++) {
            choice = rand.nextInt(1, 11);
            x = scaleVal(rand.nextDouble(this.width + 1));
            y = scaleVal(rand.nextDouble(this.length + 1));

            if (choice < 5 && p > 0) {
                nodes.add(new GeneratorNode(x, y));
                p--;
            } else {
                nodes.add(new StorageNode(x, y));
            }
        }
        return nodes;
    }

    private double scaleVal(double val) {
        return Math.floor(val * 10) / 10;
    }

    private Map<SensorNode, Set<SensorNode>> initGraph(List<SensorNode> nodes, double transmissionRange) {
        Map<SensorNode, Set<SensorNode>> graph = new HashMap<>();

        /* Create the adjacency graph */
        SensorNode node1;
        SensorNode node2;
        for (int index1 = 0; index1 < nodes.size(); index1++) {
            node1 = nodes.get(index1);

            /* Populate the graph with adjacent nodes */
            graph.putIfAbsent(node1, new HashSet<>());
            for (int index2 = index1 + 1; index2 < nodes.size(); index2++) {
                node2 = nodes.get(index2);
                graph.putIfAbsent(node2, new HashSet<>());
                if (node1.inRangeOf(node2, transmissionRange)) {
                    graph.get(node1).add(node2);
                    graph.get(node2).add(node1);
                }
            }
        }
        return graph;
    }

    @Override
    public List<SensorNode> getSensorNodes() {
        return null;
    }

    @Override
    public List<SensorNode> getGeneratorNodes() {
        return null;
    }

    @Override
    public List<SensorNode> getStorageNodes() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
