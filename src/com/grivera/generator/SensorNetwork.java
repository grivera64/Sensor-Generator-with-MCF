package com.grivera.generator;

import com.grivera.util.Pair;
import com.grivera.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * An implementation of a com.grivera.generator.Network that contains Data and Storage Sensor Nodes
 * @see Network
 */
public class SensorNetwork implements Network {

    private static final int BITS_PER_PACKET = 3200;
    private static final double E_elec = 100e-9;
    private static final double E_amp = 100e-12;

    private List<SensorNode> nodes;
    private List<DataNode> dNodes;
    private List<StorageNode> sNodes;
    private Map<SensorNode, Set<SensorNode>> graph;

    private final Map<Pair<SensorNode, SensorNode>, Integer> costMap = new HashMap<>();

    private final double width, length;
    private int dataPacketCount;
    private int storageCapacity;
    private final double transmissionRange;

    /**
     * Constructor to create a Sensor com.grivera.generator.Network
     * @param x the width of the network (in meters)
     * @param y the length of the network (in meters)
     * @param N the number of nodes
     * @param tr the transmission range of the nodes (in meters)
     * @param p the number of Data Nodes in the network
     * @param q the number of data packets each Data Node has
     * @param m the storage capacity each Storage nodes has
     */
    public SensorNetwork(double x, double y, int N, double tr, int p, int q, int m) {
        this.width = x;
        this.length = y;
        this.dataPacketCount = q;
        this.storageCapacity = m;
        this.transmissionRange = tr;

        /* Used to separate each type of node for later use and retrieval */
        this.dNodes = new ArrayList<>(p);
        this.sNodes = new ArrayList<>(N - p);

        /* Init the Sensor com.grivera.generator.Network to allow basic operations on it */
        this.nodes = this.initNodes(N, p);
        this.graph = this.initGraph(this.nodes);
    }

    /**
     * Copy constructor to create a Sensor com.grivera.generator.Network from an .sn file.
     *
     * <p></p>
     *
     * The file must follow the following format:
     * <p></p>
     * width length
     * <p>
     * data_packets_per_node storage_capacity_per_node
     * <p>
     * total_nodes transmission_range
     * <p>
     * (d/s) id x y
     * <p>
     * ...
     *
     * @param fileName the path to the .sn file
     */
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
            this.transmissionRange = fileScanner.nextDouble();
            fileScanner.nextLine();

            this.dataPacketCount = fileScanner.nextInt();
            this.storageCapacity = fileScanner.nextInt();
            fileScanner.nextLine();

            int N = fileScanner.nextInt();
            fileScanner.nextLine();

            SensorNode.resetCounter();
            StorageNode.resetCounter();
            DataNode.resetCounter();

            this.nodes = new ArrayList<>(N);
            this.sNodes = new ArrayList<>(N);
            this.dNodes = new ArrayList<>(N);

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
                    case "d" -> new DataNode(x, y, this.transmissionRange, this.dataPacketCount);
                    case "s" -> new StorageNode(x, y, this.transmissionRange, this.storageCapacity);
                    default -> throw new IOException();
                };

                this.nodes.add(node);
                if (node instanceof DataNode) {
                    this.dNodes.add((DataNode) node);
                } else {
                    this.sNodes.add((StorageNode) node);
                }
            }
            this.graph = this.initGraph(this.nodes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file provided!");
        }
    }

    public static SensorNetwork of(double x, double y, int N, double tr, int p, int q, int m) {
        SensorNetwork network;
        int attempts = 0;
        do {
            network = new SensorNetwork(x, y, N, tr, p, q, m);

            if (!network.isFeasible() || attempts > N * 1000) {
                System.out.println("Invalid network parameters! Please re-run the program.");
                System.out.println("Exiting the program...");
                System.exit(0);
            }
            attempts++;

        } while(!(network.isConnected()));

        return network;
    }

    public static SensorNetwork from(String fileName) {
        return new SensorNetwork(fileName);
    }

    public static SensorNetwork from(String fileName, int overflowPackets, int storageCapacity) {
        SensorNetwork sn = new SensorNetwork(fileName);
        sn.setOverflowPackets(overflowPackets);
        sn.setStorageCapacity(storageCapacity);
        return sn;
    }

    private List<SensorNode> initNodes(int nodeCount, int p) {
        List<SensorNode> nodes = new ArrayList<>(nodeCount);
        Random rand = new Random();

        /* Reset Counters (This is a temporary fix) */
        SensorNode.resetCounter();
        StorageNode.resetCounter();
        DataNode.resetCounter();

        /* Choose p random nodes to be Generator Nodes, the rest are Storage Nodes */
        int choice;
        double x, y;
        SensorNode tmp;
        for (int index = 0; index < nodeCount; index++) {
            choice = rand.nextInt(1, 11);
            x = this.width * rand.nextDouble();
            y = this.length * rand.nextDouble();

            if ((choice < 5 && p > 0) || nodeCount - index <= p) {
                tmp = new DataNode(x, y, this.transmissionRange, this.dataPacketCount);
                this.dNodes.add((DataNode) tmp);
                p--;
            } else {
                tmp = new StorageNode(x, y, this.transmissionRange, this.storageCapacity);
                this.sNodes.add((StorageNode) tmp);
            }
            nodes.add(tmp);
        }
        return nodes;
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
    public List<DataNode> getDataNodes() {
        return Collections.unmodifiableList(this.dNodes);
    }

    @Override
    public List<StorageNode> getStorageNodes() {
        return Collections.unmodifiableList(this.sNodes);
    }

    /**
     * Tests whether all the nodes are directly or indirectly connected with each other.
     *
     * @return true if and only if all the nodes are directly or indirectly connected
     * with each other; otherwise false
     */
    @Override
    public boolean isConnected() {
        return dfs(this.nodes);
    }

    /**
     * Tests whether there is enough storage for all the overflow packets in the network.
     *
     * @return true if and only if there is enough storage for all the overflow packets in the network;
     * otherwise false
     */
    @Override
    public boolean isFeasible() {
        int p = this.dNodes.size();
        return p * this.dataPacketCount <= (this.nodes.size() - p) * this.storageCapacity;
    }

    @Override
    public Map<SensorNode, Set<SensorNode>> getAdjacencyList() {
        return Collections.unmodifiableMap(this.graph);
    }

    @Override
    public int calculateMinCost(SensorNode from, SensorNode to) {
        Pair<SensorNode, SensorNode> pair = Pair.of(from, to);
        if (costMap.containsKey(pair)) {
            return costMap.get(pair);
        }

        int cost = this.calculateCostOfPath(this.getMinCostPath(from, to));
        costMap.put(pair, cost);
        return cost;
    }

    /**
     * Returns the sensor nodes in the min-cost path between the from and to sensor nodes
     *
     * @param from the starting sensor node
     * @param to the ending sensor node
     * @return a list of the sensor nodes in the min-cost path between the from and to sensor nodes
     */
    @Override
    public List<SensorNode> getMinCostPath(SensorNode from, SensorNode to) {
        return bfs(this.graph, from, to);
    }

    /**
     * Calculates the cost of a given path.
     * @param path the path between two sensor nodes
     * @return the cost of the given path
     */
    @Override
    public int calculateCostOfPath(List<SensorNode> path) {
        int currCost = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            currCost += this.getCost(path.get(i), path.get(i + 1));
        }
        return currCost;
    }

    /**
     * Saves the network into a .sn file format.
     *
     * @param fileName the path to the file to save to
     */
    @Override
    public void save(String fileName) {
        File file = new File(fileName);

        try (PrintWriter pw = new PrintWriter(file)) {
            pw.printf("%f %f %f\n", this.getWidth(), this.getLength(), this.transmissionRange);   // X, Y, Tr
            pw.printf("%d %d\n", this.dataPacketCount, this.storageCapacity);  // q m
            pw.printf("%d %d\n", this.nodes.size(), this.dNodes.size());       // N p

            for (SensorNode n : this.nodes) {
                pw.printf("%s %f %f\n", (n instanceof DataNode) ? 'd' : 's', n.getX(), n.getY());
            }
            System.out.printf("Saved sensor network in file \"%s\"!\n", fileName);
        } catch (IOException e) {
            System.out.printf("ERROR: Failed to create \"%s\"!\n", fileName);
        }
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

    /**
     * Saves the network in the <b>DIMAC</b> format
     * that can be used for the min-cost flow program <a href="https://github.com/iveney/cs2">CS2</a>.
     *
     * @param fileName the path to the file to save to
     */
    @Override
    public void saveAsCsInp(String fileName) {
        final int supply = this.dataPacketCount * this.dNodes.size();
        final int demand = -supply;
        final int minFlow = 0;
        final int maxFlow = this.dataPacketCount;

        File file = new File(fileName);
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
            for (SensorNode dn : this.dNodes) {
                writer.printf("a %d %d %d %d %d\n", 0, dn.getUuid(), minFlow, maxFlow, 0);
            }

            /* Find all paths from DN#->SN# */
            List<SensorNode> path;
            int currCost;
            for (SensorNode dn : this.dNodes) {
                for (SensorNode sn : this.sNodes) {
                    path = this.bfs(this.graph, dn, sn);
                    currCost = this.calculateCostOfPath(path);
                    writer.printf("a %d %d %d %d %d\n", dn.getUuid(), sn.getUuid(), minFlow, maxFlow, currCost);
                }
            }

            /* Path from SN to Sink is always 0 cost (not represented in the network) */
            for (SensorNode sn : this.sNodes) {
                writer.printf("a %d %d %d %d %d\n",
                        sn.getUuid(), this.nodes.size() + 1, minFlow, this.storageCapacity, 0);
            }
            System.out.printf("Saved flow network in file \"%s\"!\n", fileName);
        } catch (IOException e) {
            System.out.printf("ERROR: Failed to create %s\n", fileName);
        }
    }

    private List<SensorNode> bfs(Map<SensorNode, Set<SensorNode>> graph, SensorNode start, SensorNode end) {
        Queue<Tuple<SensorNode, Integer, SensorNode>> q = new PriorityQueue<>(Comparator.comparing(Tuple::second));
        Map<SensorNode, SensorNode> backPointers = new HashMap<>();
        q.offer(Tuple.of(start, 0, null));

        Tuple<SensorNode, Integer, SensorNode> currPair;
        SensorNode curr;
        SensorNode prev;
        int value;
        while (!q.isEmpty()) {
            currPair = q.poll();
            curr = currPair.first();
            value = currPair.second();
            prev = currPair.third();

            if (!backPointers.containsKey(curr)) {
                backPointers.put(curr, prev);
                for (SensorNode neighbor : graph.getOrDefault(curr, Set.of())) {
                    q.offer(Tuple.of(neighbor, value + this.getCost(curr, neighbor), curr));
                }
            }

            if (curr.equals(end)) {
                break;
            }
        }

        LinkedList<SensorNode> deque = new LinkedList<>();
        curr = end;
        while (curr != null) {
            deque.push(curr);
            curr = backPointers.getOrDefault(curr, null);
        }

        return deque;
    }

    private int getCost(SensorNode from, SensorNode to) {
        double cost = BITS_PER_PACKET * (2 * E_elec + E_amp * Math.pow(from.distanceTo(to), 2));
        return (int) Math.round(cost * Math.pow(10, 6));
    }

    private int getEdgeCount() {
        return this.nodes.size() + this.sNodes.size() * this.dNodes.size();
    }

    public void setOverflowPackets(int overflowPackets) {
        this.dataPacketCount = overflowPackets;

        for (DataNode dn : this.dNodes) {
            dn.setOverflowPackets(overflowPackets);
        }
    }

    public void setStorageCapacity(int storageCapacity) {
        this.storageCapacity = storageCapacity;

        for (StorageNode sn : this.sNodes) {
            sn.setCapacity(storageCapacity);
        }
    }

    @Override
    public boolean canSendPackets(DataNode dn, StorageNode sn, int packets) {
        return dn.canRemovePackets(packets) && sn.canStore(packets);
    }

    @Override
    public void sendPackets(DataNode dn, StorageNode sn, int packets) {
        if (!this.canSendPackets(dn, sn, packets)) {
            throw new IllegalArgumentException(
                    String.format("Cannot send from %s (%d/%d packets left) -> %s (%d/%d space left)\n",
                            dn.getName(), dn.getPacketsLeft(), this.dataPacketCount,
                            sn.getName(), sn.getSpaceLeft(), this.storageCapacity
                    )
            );
        }

        dn.removePackets(packets);
        sn.storePackets(packets);
    }

    @Override
    public void resetPackets() {
        for (DataNode dn : this.dNodes) {
            dn.resetPackets();
        }

        for (StorageNode sn : this.sNodes) {
            sn.resetPackets();
        }
    }
}
