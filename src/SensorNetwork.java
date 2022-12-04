import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

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
                    graph.get(node2).add(node1); // This makes the graph a non-directed graph
                }
            }
        }
        return graph;
    }

    @Override
    public double getWidth() {
        return this.width;
    }

    @Override
    public double getLength() {
        return this.length;
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

    @Override
    public Map<SensorNode, Set<SensorNode>> getAdjacencyLists() {
        return new HashMap<>(this.graph);
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
    public void saveAsCsInp(String fileName, int srcId, int sinkId) {
        final int supply = this.dataPacketCount * this.gNodes.size();
        final int demand = -supply;
        final int minFlow = 0;
        final int maxFlow = this.dataPacketCount;

        File file = new File(fileName + ".inp");
        try (PrintWriter writer = new PrintWriter(file)) {
            /* Header */
            writer.printf("c Min-Cost flow problem with %d nodes and %d arcs (edges)\n",
                    this.nodes.size(), this.getEdgeCount());
            writer.printf("p min %d %d\n",
                    this.nodes.size(), this.getEdgeCount());
            writer.println();

            /* Set s (source) and t (sink) nodes */
            SensorNode source = this.gNodes.get(srcId - 1);
            writer.printf("c Supply of %d at node %d (%s)\n", supply, source.getUuid(), source.getName());
            writer.printf("n %d %d\n", source.getUuid(), supply);
            writer.println();

            SensorNode sink = this.sNodes.get(sinkId - 1);
            writer.printf("c Demand of %d at node %d (%s)\n", demand, sink.getUuid(), sink.getName());
            writer.printf("n %d %d\n", sink.getUuid(), demand);
            writer.println();

            /* Arcs */
            writer.println("c arc list follows");
            writer.println("c arc has <tail> <head> <capacity l.b.> <capacity u.b> <cost>");

            /* Attempt 1 */

//            Set<SensorNode> seen = new HashSet<>();
//            SensorNode from;
//            for (Map.Entry<SensorNode, Set<SensorNode>> entry : this.graph.entrySet()) {
//                from = entry.getKey();
//
//                if (from.equals(sink)) {
//                    continue;
//                }
//
//                seen.add(from);
//                for (SensorNode to : entry.getValue()) {
//
//                    if (to.equals(source)) {
//                        continue;
//                    }
//
//                    if (seen.contains(to)) {
//                        continue;
//                    }
//
//                    writer.printf("c %s <=> %s\n", from.getName(), to.getName());
//                    writer.printf("a %-2d %-2d %-2d %-2d %-2d\n",
//                            from.getUuid(),
//                            to.getUuid(),
//                            minFlow,
//                            maxFlow,
//                            (from.equals(source) || to.equals(sink)) ? 0 : this.getCost(from, to, this.dataPacketCount)
//                    );
//                }
//            }

            /* Attempt 2 */
//            Queue<SensorNode> q = new ArrayDeque<>();
//            Set<SensorNode> seen = new HashSet<>();
//            seen.add(source);
//
//            Set<SensorNode> dataNodes = this.graph.getOrDefault(source, null)
//                    .stream()
//                    .filter(sensorNode -> sensorNode instanceof GeneratorNode)
//                    .collect(Collectors.toSet());
//            for (SensorNode to : dataNodes) {
//                writer.printf("a %-2d %-2d %-2d %-2d %-2d\n", source.getUuid(), to.getUuid(), minFlow, maxFlow, 0);
//                q.offer(to);
//            }
//
//            SensorNode from;
//            while (!q.isEmpty()) {
//                from = q.poll();
//
//                if (seen.contains(from) || from.equals(sink)) {
//                    continue;
//                }
//
//                seen.add(from);
//                for (SensorNode to : dataNodes) {
//                    writer.printf("a %-2d %-2d %-2d %-2d %-2d\n",
//                            from.getUuid(), to.getUuid(), minFlow, maxFlow,
//                            this.getCost(from, to, this.dataPacketCount)
//                    );
//                    q.offer(to);
//                }
//            }
            System.out.println("Saved file!");
        } catch (IOException e) {
            System.out.printf("ERROR: Failed to create %s.inp\n", fileName);
        }
    }

    private int getCost(SensorNode from, SensorNode to, int k) {
        final int elec = 100;
        final int amp = 100;

        double transmission = 2 * elec * k;
        double receiving = amp * k * Math.pow(from.distanceTo(to), 2);
        return (int) Math.ceil(transmission + receiving);
    }

    private int getEdgeCount() {
        return (this.graph.values().stream().mapToInt(Set::size).sum() / 2) - 3;
    }
}
