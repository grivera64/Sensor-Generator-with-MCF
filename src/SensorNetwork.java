import com.sun.javafx.scene.shape.ArcHelper;

import java.util.*;

public class SensorNetwork implements Network {

    private List<SensorNode> nodes;
    private List<SensorNode> gNodes;
    private List<SensorNode> sNodes;
    private Map<SensorNode, Set<SensorNode>> graph;

    private final double width, length;
    private final int dataPacketCount;
    private final int storageCapacity;

    public SensorNetwork(double x, double y, int N, double tr, int p, int q, int m) {
        this.width = x;
        this.length = y;
        this.dataPacketCount = q;
        this.storageCapacity = m;

        /* Used to separate each type of node for later use and retrieval */
        this.gNodes = new ArrayList<>(p);
        this.sNodes = new ArrayList<>(N - p);

        /* Init the Sensor Network to allow basic operations on it */
        this.nodes = this.initNodes(N, p);
        this.graph = this.initGraph(this.nodes, tr);
    }

    private List<SensorNode> initNodes(int nodeCount, int p) {
        List<SensorNode> nodes = new ArrayList<>(nodeCount);
        Random rand = new Random();

        /* Choose p random nodes to be Generator Nodes, the rest are Storage Nodes */
        int choice;
        double x, y;
        SensorNode tmp;
        for (int index = 0; index < nodeCount; index++) {
            choice = rand.nextInt(1, 11);
            x = scaleVal(rand.nextDouble(this.width + 1));
            y = scaleVal(rand.nextDouble(this.length + 1));

            if (choice < 5 && p > 0) {
                tmp = new GeneratorNode(x, y);
                this.gNodes.add(tmp);
                p--;
            } else {
                tmp = new StorageNode(x, y);
                this.sNodes.add(tmp);
            }
            nodes.add(tmp);
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
        return new ArrayList<>(this.nodes);
    }

    @Override
    public List<SensorNode> getGeneratorNodes() {
        return new ArrayList<>(this.gNodes);
    }

    @Override
    public List<SensorNode> getStorageNodes() {
        return new ArrayList<>(this.sNodes);
    }

    @Override
    public boolean isConnected() {
        return dfs(this.nodes);
    }

    @Override
    public boolean isFeasible() {
        int p = this.gNodes.size();
        return p * this.dataPacketCount <= (this.nodes.size() - p) * this.storageCapacity;
    }

    private boolean dfs(List<SensorNode> nodes) {
        Stack<SensorNode> stack = new Stack<>();
        Set<SensorNode> seen = new HashSet<>();
        stack.push(nodes.get(0));

        SensorNode curr;
        while (!stack.isEmpty()) {
            curr = stack.pop();
            seen.add(curr);

            for (SensorNode neighbor : this.graph.getOrDefault(curr, Set.of())) {
                if (!seen.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
        return seen.size() == nodes.size();
    }

    @Override
    public void saveAsCsInp(String fileName) {

    }
}
