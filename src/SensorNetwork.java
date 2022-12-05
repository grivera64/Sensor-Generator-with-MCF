import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
        this.nodes = this.initNodes(N, p, tr);
        this.graph = this.initGraph(this.nodes, tr);
    }

    public SensorNetwork(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("File \"%s\" doesn't exist!", fileName));
        }

        try (Scanner fileScanner = new Scanner(file)) {
            if (!fileScanner.hasNext()) {
                throw new IllegalArgumentException(String.format("File \"%s\" is empty!", fileName));
            }

            this.width = fileScanner.nextDouble();
            this.length = fileScanner.nextDouble();
            fileScanner.nextLine();

            this.dataPacketCount = fileScanner.nextInt();
            this.storageCapacity = fileScanner.nextInt();
            fileScanner.nextLine();

            int N = fileScanner.nextInt();
            int Tr = fileScanner.nextInt();
            fileScanner.nextLine();

            this.nodes = new ArrayList<>(N);
            this.sNodes = new ArrayList<>(N);
            this.gNodes = new ArrayList<>(N);

            String[] lineArgs;
            double x, y;
            SensorNode node;
            for (int i = 0; i < N; i++) {
                lineArgs = fileScanner.nextLine().split(" ");
                if (lineArgs.length != 3) {
                    throw new IOException();
                }

                x = Double.parseDouble(lineArgs[1]);
                y = Double.parseDouble(lineArgs[2]);

                // Requires JDK 12+
                node = switch (lineArgs[0]) {
                    case "d" -> new GeneratorNode(x, y, Tr);
                    case "s" -> new StorageNode(x, y, Tr);
                    default -> throw new IOException();
                };

                this.nodes.add(node);
                if (node instanceof GeneratorNode) {
                    this.gNodes.add(node);
                } else {
                    this.sNodes.add(node);
                }
            }
            this.graph = this.initGraph(this.nodes, Tr);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file provided!");
        }
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

            if ((choice < 5 && p > 0) || nodeCount - index < p) {
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
    }
}
