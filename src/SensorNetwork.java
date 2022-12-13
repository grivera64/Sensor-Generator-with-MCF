import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class SensorNetwork implements Network {

    private static final int dataPacketBitCount = 3200;
    private static final double E_elec = 100e-9;
    private static final double E_amp = 100e-12;

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
        this.nodes = this.initNodes(N, p, tr);
        this.graph = this.initGraph(this.nodes);
    }

    private List<SensorNode> initNodes(int nodeCount, int p, double tr) {
        List<SensorNode> nodes = new ArrayList<>(nodeCount);
        Random rand = new Random();

        /* Reset Counters (This is a temporary fix) */
        SensorNode.resetCounter();
        StorageNode.resetCounter();
        GeneratorNode.resetCounter();

        /* Choose p random nodes to be Generator Nodes, the rest are Storage Nodes */
        int choice;
        double x, y;
        SensorNode tmp;
        for (int index = 0; index < nodeCount; index++) {
            choice = rand.nextInt(1, 11);
            x = scaleVal(rand.nextDouble(this.width + 1));
            y = scaleVal(rand.nextDouble(this.length + 1));

            if ((choice < 5 && p > 0) || nodeCount - index <= p) {
                tmp = new GeneratorNode(x, y, tr);
                this.gNodes.add(tmp);
                p--;
            } else {
                tmp = new StorageNode(x, y, tr);
                this.sNodes.add(tmp);
            }
            nodes.add(tmp);
        }
        return nodes;
    }

    private double scaleVal(double val) {
        return Math.floor(val * 10) / 10;
    }

    private Map<SensorNode, Set<SensorNode>> initGraph(List<SensorNode> nodes) {
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
                if (node1.inRangeOf(node2)) {
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
        return Collections.unmodifiableList(this.nodes);
    }

    @Override
    public List<SensorNode> getGeneratorNodes() {
        return Collections.unmodifiableList(this.gNodes);
    }

    @Override
    public List<SensorNode> getStorageNodes() {
        return Collections.unmodifiableList(this.sNodes);
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
        return Collections.unmodifiableMap(this.graph);
    }

    @Override
    public List<SensorNode> getMinCostPath(SensorNode from, SensorNode to) {
        return bfs(this.graph, from, to);
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
        final int supply = this.dataPacketCount * this.gNodes.size();
        final int demand = -supply;
        final int minFlow = 0;
        final int maxFlow = this.dataPacketCount;

        File file = new File(fileName + ".inp");
        try (PrintWriter writer = new PrintWriter(file)) {
            /* Header */
            writer.printf("c Min-Cost flow problem with %d nodes and %d arcs (edges)\n",
                    this.nodes.size() + 2, this.getEdgeCount());
            writer.printf("p min %d %d\n",
                    this.nodes.size() + 2, this.getEdgeCount());
            writer.println();

            /* Set s (source) and t (sink) nodes */
            writer.printf("c Supply of %d at node %d (\"Source\")\n", supply, 0);
            writer.printf("n %d %d\n", 0, supply);
            writer.println();

            writer.printf("c Demand of %d at node %d (\"Sink\")\n", demand, this.nodes.size() + 1);
            writer.printf("n %d %d\n", this.nodes.size() + 1, demand);
            writer.println();

            /* Arcs */
            writer.println("c arc list follows");
            writer.println("c arc has <tail> <head> <capacity l.b.> <capacity u.b> <cost>");

            /* Path from Source to DN is always 0 cost (not represented in the network) */
            for (SensorNode dn : this.gNodes) {
                writer.printf("a %d %d %d %d %d\n", 0, dn.getUuid(), minFlow, maxFlow, 0);
            }

            /* Find all paths from DN#->SN# */
            List<SensorNode> path;
            int currCost;
            for (SensorNode dn : this.gNodes) {
                for (SensorNode sn : this.sNodes) {
                    path = this.bfs(this.graph, dn, sn);
                    currCost = 0;
                    for (int i = 0; i < path.size() - 1; i++) {
                        currCost += this.getCost(path.get(i), path.get(i + 1), dataPacketBitCount);
                    }
                    writer.printf("a %d %d %d %d %d\n",
                            dn.getUuid(), sn.getUuid(), minFlow, maxFlow,
                            currCost);
                }
            }

            /* Path from SN to Sink is always 0 cost (not represented in the network) */
            for (SensorNode sn : this.sNodes) {
                writer.printf("a %d %d %d %d %d\n", sn.getUuid(), this.nodes.size() + 1, minFlow, maxFlow, 0);
            }
            System.out.printf("Saved flow network in file \"%s.inp\"!\n", fileName);
        } catch (IOException e) {
            System.out.printf("ERROR: Failed to create %s.inp\n", fileName);
        }
    }

    private List<SensorNode> bfs(Map<SensorNode, Set<SensorNode>> graph, SensorNode start, SensorNode end) {
        Queue<Pair<SensorNode, Integer>> q = new PriorityQueue<>(Comparator.comparing(Pair::getValue));
        Set<SensorNode> seen = new HashSet<>();
        Map<SensorNode, SensorNode> backPointers = new HashMap<>();
        Map<SensorNode, Integer> minCost = new HashMap<>();

        q.offer(new Pair<>(start, 0));
        backPointers.put(start, null);

        Pair<SensorNode, Integer> currPair;
        SensorNode curr = null;
        int value;
        while (!q.isEmpty()) {
            currPair = q.poll();
            curr = currPair.getKey();
            value = currPair.getValue();

            if (seen.contains(curr)) {
                continue;
            }

            if (curr.equals(end)) {
                break;
            }

            seen.add(curr);
            for (SensorNode neighbor : graph.getOrDefault(curr, Set.of())) {
                if (seen.contains(neighbor)) {
                    continue;
                }

                value += this.getCost(curr, neighbor, dataPacketBitCount);
                q.offer(new Pair<>(neighbor, value));

                if (!backPointers.containsKey(neighbor) || value <= minCost.get(neighbor)) {
                    backPointers.put(neighbor, curr);
                    minCost.put(neighbor, value);
                }
            }
        }

        LinkedList<SensorNode> deque = new LinkedList<>();
        while (curr != null) {
            deque.push(curr);
            curr = backPointers.getOrDefault(curr, null);
        }

        return deque;
    }

    private int getCost(SensorNode from, SensorNode to, int k) {
        double cost = (2 * E_elec * k) + (E_amp * k * Math.pow(from.distanceTo(to), 2));
        return (int) Math.round(cost * Math.pow(10, 6));
    }

    private int getEdgeCount() {
        return this.sNodes.size() * this.gNodes.size() + this.sNodes.size() + this.gNodes.size();
    }
}
