package com.grivera.generator;

import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.Pair;
import com.grivera.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

//imports for google OR-tools
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

/**
 * An implementation of a com.grivera.generator.Network that contains Data and
 * Storage Sensor Nodes
 * 
 * @see Network
 */
public class SensorNetwork implements Network {

    private List<SensorNode> nodes;
    private List<DataNode> dNodes;
    private List<StorageNode> sNodes;
    private Map<SensorNode, Set<SensorNode>> graph;

    private final Map<Pair<SensorNode, SensorNode>, Integer> costMap = new HashMap<>();

    private final double width, length;
    private int dataPacketCount;
    private int storageCapacity;
    private final double transmissionRange;
    private int batteryCapacity;

    /**
     * Constructor to create a Sensor com.grivera.generator.Network
     * 
     * @param x  the width of the network (in meters)
     * @param y  the length of the network (in meters)
     * @param N  the number of nodes
     * @param tr the transmission range of the nodes (in meters)
     * @param p  the number of Data Nodes in the network
     * @param q  the number of data packets each Data Node has
     * @param m  the storage capacity each Storage nodes has
     * @param c  the battery capacity of each Sensor node (in micro Joules)
     */
    public SensorNetwork(double x, double y, int N, double tr, int p, int q, int m, int c) {
        this.width = x;
        this.length = y;
        this.dataPacketCount = q;
        this.storageCapacity = m;
        this.transmissionRange = tr;
        this.batteryCapacity = c;

        /* Used to separate each type of node for later use and retrieval */
        this.dNodes = new ArrayList<>(p);
        this.sNodes = new ArrayList<>(N - p);

        /*
         * Init the Sensor com.grivera.generator.Network to allow basic operations on it
         */
        this.nodes = this.initNodes(N, p);
        this.graph = this.initGraph(this.nodes);
    }

    /**
     * Copy constructor to create a Sensor com.grivera.generator.Network from an .sn
     * file.
     *
     * <p>
     * </p>
     *
     * The file must follow the following format:
     * <p>
     * </p>
     * width length transmission_range
     * <p>
     * data_packets_per_node storage_capacity_per_node
     * <p>
     * total_nodes battery_capacity_per_node
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
            this.batteryCapacity = fileScanner.nextInt();
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
                    case "d" -> new DataNode(x, y, this.transmissionRange, this.batteryCapacity, this.dataPacketCount);
                    case "s" ->
                        new StorageNode(x, y, this.transmissionRange, this.batteryCapacity, this.storageCapacity);
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

    public static SensorNetwork of(double x, double y, int N, double tr, int p, int q, int m, int c) {
        SensorNetwork network;
        int attempts = 0;
        do {
            network = new SensorNetwork(x, y, N, tr, p, q, m, c);

            /* Checks if the parameters in the program are feasible */
            if (!network.isFeasible()) {
                System.out.println("Invalid network parameters! Please re-run the program.");
                System.out.println("Exiting the program...");
                System.exit(0);
            }

            /*
             * Checks if we were able to find a valid network within a reasonable range of
             * attempts
             */
            if (attempts > N * 1000) {
                System.out.printf("Failed to create a connected network after %d tries! Please re-run the program.\n",
                        N * 1000);
                System.out.println("Exiting the program...");
                System.exit(0);
            }
            attempts++;

        } while (!(network.isConnected()));

        return network;
    }

    public static SensorNetwork from(String fileName) {
        return new SensorNetwork(fileName);
    }

    public static SensorNetwork from(String fileName, int overflowPackets, int storageCapacity, int batteryCapacity) {
        SensorNetwork sn = new SensorNetwork(fileName);
        sn.setOverflowPackets(overflowPackets);
        sn.setStorageCapacity(storageCapacity);
        sn.setBatteryCapacity(batteryCapacity);
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
                tmp = new DataNode(x, y, this.transmissionRange, this.batteryCapacity, this.dataPacketCount);
                this.dNodes.add((DataNode) tmp);
                p--;
            } else {
                tmp = new StorageNode(x, y, this.transmissionRange, this.batteryCapacity, this.storageCapacity);
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
     * Tests whether all the nodes are directly or indirectly connected with each
     * other.
     *
     * @return true if and only if all the nodes are directly or indirectly
     *         connected
     *         with each other; otherwise false
     */
    @Override
    public boolean isConnected() {
        return dfs(this.nodes);
    }

    /**
     * Tests whether there is enough storage for all the overflow packets in the
     * network.
     *
     * @return true if and only if there is enough storage for all the overflow
     *         packets in the network;
     *         otherwise false
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
     * Returns the sensor nodes in the min-cost path between the from and to sensor
     * nodes
     *
     * @param from the starting sensor node
     * @param to   the ending sensor node
     * @return a list of the sensor nodes in the min-cost path between the from and
     *         to sensor nodes
     */
    @Override
    public List<SensorNode> getMinCostPath(SensorNode from, SensorNode to) {
        return bfs(this.graph, from, to);
    }

    /**
     * Calculates the cost of a given path.
     * 
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
            pw.printf("%f %f %f\n", this.getWidth(), this.getLength(), this.transmissionRange); // X, Y, Tr
            pw.printf("%d %d\n", this.dataPacketCount, this.storageCapacity); // q m
            pw.printf("%d %d\n", this.nodes.size(), this.batteryCapacity); // N c

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

            for (SensorNode neighbor : this.getNeighbors(curr)) {
                if (!seen.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }
        return seen.size() == nodes.size();
    }

    private Set<SensorNode> getNeighbors(SensorNode node) {
        return this.graph.getOrDefault(node, Set.of());
    }

    private boolean isConnected(SensorNode sensorNode1, SensorNode sensorNode2) {
        return this.getNeighbors(sensorNode1).contains(sensorNode2);
    }

    /*
     * Calculates MCF using ILP with google OR-tools
     *
     */
    @Override
    public int getMinCostIlp() {
        int infinity = java.lang.Integer.MAX_VALUE;
        int n = this.nodes.size();
        int sink = 2 * n + 1;
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");
        MPVariable[][] x = new MPVariable[2 * n + 2][2 * n + 2];
        int[][] c = new int[2 * n + 2][2 * n + 2];

        // create 2d array of decision variable x
        // x_i_j value represents number of flows from i to j

        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                x[i][j] = solver.makeIntVar(0, infinity, "x_" + i + "_" + j);
            }
        }
        // if i has no edge to j, x_i_j=0
        makeMaxFlowEdges(x, solver);
        makeCostArray(c);
        // constraint (11):
        // indicates the maximum number of packets data node i can offload is di, the
        // initial number of data packets data node i has
        MPConstraint[] eleven = new MPConstraint[this.dNodes.size()];
        int constraint = 0;
        for (DataNode dn : this.dNodes) {
            eleven[constraint] = solver.makeConstraint(this.dataPacketCount, this.dataPacketCount, "" + dn.getUuid());
            eleven[constraint].setCoefficient(x[0][dn.getUuid()], 1);
            // eleven[constraint].setCoefficient(solver.makeIntConst(this.dataPacketCount),
            // -1);
            constraint++;
        }

        // constraint (5):
        // indicates the maximum number of packets storage node i can store is mi, the
        // storage capacity of storage node i
        MPConstraint[] five = new MPConstraint[this.sNodes.size()];
        constraint = 0;
        for (StorageNode sn : this.sNodes) {
            five[constraint] = solver.makeConstraint(-infinity, this.storageCapacity, "" + (sn.getUuid() + n));
            five[constraint].setCoefficient(x[sn.getUuid() + n][sink], 1);
            constraint++;
        }

        // constraint (6):
        // the flow conservation for data nodes, where the number of its own data
        // packets offloaded plus the number of data packets it relays for other data
        // nodes equals the number of data packets it transmits.
        MPConstraint[] six = new MPConstraint[this.dNodes.size()];
        constraint = 0;
        for (DataNode dn : this.dNodes) {
            six[constraint] = solver.makeConstraint(0, 0);
            six[constraint].setCoefficient(x[0][dn.getUuid()], 1);
            for (SensorNode sn : this.nodes) {
                six[constraint].setCoefficient(x[sn.getUuid()][dn.getUuid()], 1);
                six[constraint].setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);

                six[constraint].setCoefficient(x[dn.getUuid() + n][sn.getUuid()], -1);
                six[constraint].setCoefficient(x[dn.getUuid() + n][sn.getUuid() + n], -1);
            }
            constraint++;
        }

        // constraint (7):
        // the flow conservation for storage nodes, which says that data packets a
        // storage node receives are either relayed to other nodes or stored by this
        // storage node
        MPConstraint[] seven = new MPConstraint[this.sNodes.size()];
        constraint = 0;
        for (StorageNode sn : this.sNodes) {
            seven[constraint] = solver.makeConstraint(0, 0);
            seven[constraint].setCoefficient(x[sn.getUuid() + n][sink], -1);
            for (SensorNode snp : this.nodes) {
                seven[constraint].setCoefficient(x[snp.getUuid()][sn.getUuid()], 1);
                seven[constraint].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], 1);

                seven[constraint].setCoefficient(x[sn.getUuid() + n][snp.getUuid()], -1);
                seven[constraint].setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], -1);
            }
            constraint++;
        }

        // constraint(8):
        // (8) and (9) represents the energy constraints for data nodes and storage
        // nodes respectively
        // in our work we don't consider the storage cost, so its just one constraint
        MPConstraint[] eight = new MPConstraint[this.nodes.size()];
        constraint = 0;
        for (SensorNode sn : this.nodes) {
            eight[constraint] = solver.makeConstraint(-infinity, sn.getEnergy());
            for (SensorNode snp : this.nodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                eight[constraint].setCoefficient(x[snp.getUuid()][sn.getUuid()], sn.calculateReceivingCost());
                eight[constraint].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], sn.calculateReceivingCost());

                eight[constraint].setCoefficient(x[sn.getUuid() + n][snp.getUuid()],
                        sn.calculateTransmissionCost(snp));
                eight[constraint]
                        .setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], sn.calculateTransmissionCost(snp));

            }
            constraint++;
        }

        // set Objective, (maximize flow from source to data in nodes)
        /*
         * MPObjective objective = solver.objective();
         * for (DataNode dn : this.dNodes) {
         * objective.setCoefficient(x[0][dn.getUuid()], 1);
         * }
         * objective.setMaximization();
         */

        // set Objective, (minimize dvar * cost)
        MPObjective objective = solver.objective();
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                objective.setCoefficient(x[i][j], c[i][j]);
            }
        }
        objective.setMinimization();

        // solve
        final MPSolver.ResultStatus resultStatus = solver.solve();

        // return value for objective
        if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("Objective value: " + objective.value());
            return (int) objective.value();
        }
        // if result not optimal, it will return -99;
        // I don't think it should ever return -99;
        return -99;
        /*
         * System.out.println(
         * "Objective: " + objective.value() + " data packet count: " +
         * this.dNodes.size() * dataPacketCount);
         * if (objective.value() == this.dNodes.size() * dataPacketCount) {
         * return true;
         * }
         * 
         * return false;
         */

    }

    private void makeCostArray(int[][] c) {
        int infinity = java.lang.Integer.MAX_VALUE;
        int n = this.nodes.size();
        int sink = 2 * n + 1;
        // first set default cost value as infinity
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[i].length; j++) {
                c[i][j] = infinity;
            }
        }
        // calculate cost from source to data in nodes (0)
        for (DataNode dn : this.dNodes) {
            c[0][dn.getUuid()] = 0;
        }
        // calculate cost from in-node to respective out-node
        for (DataNode dn : this.dNodes) {
            c[dn.getUuid()][dn.getUuid() + n] = 0;
        }
        // calculate cost from data out-nodes to data in-nodes and storage in-nodes
        for (DataNode dn : this.dNodes) {
            for (DataNode dnp : this.dNodes) {
                if (dn.getUuid() == dnp.getUuid()) {
                    continue;
                }
                if (isConnected(dnp, dn)) {
                    int tCost = dn.calculateTransmissionCost(dnp);
                    int rCost = dnp.calculateReceivingCost();
                    c[dn.getUuid() + n][dnp.getUuid()] = tCost + rCost;
                }
            }
            for (StorageNode sn : this.sNodes) {
                if (isConnected(sn, dn)) {
                    int tCost = dn.calculateTransmissionCost(sn);
                    int rCost = sn.calculateReceivingCost();
                    c[dn.getUuid() + n][sn.getUuid()] = tCost + rCost;
                }
            }
        }
        // calculate cost from storage in-nodes to respective out-node;
        for (StorageNode sn : this.sNodes) {
            c[sn.getUuid()][sn.getUuid() + n] = 0;
        }
        // calculate cost from storage out-nodes to data in-nodes and storage in-nodes
        for (StorageNode sn : this.sNodes) {
            for (StorageNode snp : this.sNodes) {
                if (sn.getUuid() == snp.getUuid()) {
                    continue;
                }
                if (isConnected(sn, snp)) {
                    int tCost = sn.calculateTransmissionCost(snp);
                    int rCost = snp.calculateReceivingCost();
                    c[sn.getUuid() + n][snp.getUuid()] = tCost + rCost;
                }
            }
            for (DataNode dn : this.dNodes) {
                if (isConnected(dn, sn)) {
                    int tCost = sn.calculateTransmissionCost(dn);
                    int rCost = dn.calculateReceivingCost();
                    c[sn.getUuid() + n][dn.getUuid()] = tCost + rCost;
                }
            }
            // calculate cost from storage out-nodes to sink (0)
            c[sn.getUuid() + n][sink] = 0;
        }

    }

    /*
     * Checks if the max flow network is feasible
     * uses google OR-tools linear solver to solve ILP model
     * google OR-tools linear solver:
     * https://developers.google.com/optimization/mip/
     */
    @Override
    public boolean isMaxFlowFeasible() {

        int infinity = java.lang.Integer.MAX_VALUE;
        int n = this.nodes.size();
        int sink = 2 * n + 1;
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");
        MPVariable[][] x = new MPVariable[2 * n + 2][2 * n + 2];

        // create 2d array of decision variable x
        // x_i_j value represents number of flows from i to j

        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                x[i][j] = solver.makeIntVar(0, infinity, "x_" + i + "_" + j);
            }
        }
        // if i has no edge to j, x_i_j=0
        makeMaxFlowEdges(x, solver);

        // constraint (4):
        // indicates the maximum number of packets data node i can offload is di, the
        // initial number of data packets data node i has
        MPConstraint[] four = new MPConstraint[this.dNodes.size()];
        int constraint = 0;
        for (DataNode dn : this.dNodes) {
            four[constraint] = solver.makeConstraint(-infinity, this.dataPacketCount, "" + dn.getUuid());
            four[constraint].setCoefficient(x[0][dn.getUuid()], 1);
            constraint++;
        }

        // constraint (5):
        // indicates the maximum number of packets storage node i can store is mi, the
        // storage capacity of storage node i
        MPConstraint[] five = new MPConstraint[this.sNodes.size()];
        constraint = 0;
        for (StorageNode sn : this.sNodes) {
            five[constraint] = solver.makeConstraint(-infinity, this.storageCapacity, "" + (sn.getUuid() + n));
            five[constraint].setCoefficient(x[sn.getUuid() + n][sink], 1);
            constraint++;
        }

        // constraint (6):
        // the flow conservation for data nodes, where the number of its own data
        // packets offloaded plus the number of data packets it relays for other data
        // nodes equals the number of data packets it transmits.
        MPConstraint[] six = new MPConstraint[this.dNodes.size()];
        constraint = 0;
        for (DataNode dn : this.dNodes) {
            six[constraint] = solver.makeConstraint(0, 0);
            six[constraint].setCoefficient(x[0][dn.getUuid()], 1);
            for (SensorNode sn : this.nodes) {
                six[constraint].setCoefficient(x[sn.getUuid()][dn.getUuid()], 1);
                six[constraint].setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);

                six[constraint].setCoefficient(x[dn.getUuid() + n][sn.getUuid()], -1);
                six[constraint].setCoefficient(x[dn.getUuid() + n][sn.getUuid() + n], -1);
            }
            constraint++;
        }

        // constraint (7):
        // the flow conservation for storage nodes, which says that data packets a
        // storage node receives are either relayed to other nodes or stored by this
        // storage node
        MPConstraint[] seven = new MPConstraint[this.sNodes.size()];
        constraint = 0;
        for (StorageNode sn : this.sNodes) {
            seven[constraint] = solver.makeConstraint(0, 0);
            seven[constraint].setCoefficient(x[sn.getUuid() + n][sink], -1);
            for (SensorNode snp : this.nodes) {
                seven[constraint].setCoefficient(x[snp.getUuid()][sn.getUuid()], 1);
                seven[constraint].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], 1);

                seven[constraint].setCoefficient(x[sn.getUuid() + n][snp.getUuid()], -1);
                seven[constraint].setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], -1);
            }
            constraint++;
        }

        // constraint(8):
        // (8) and (9) represents the energy constraints for data nodes and storage
        // nodes respectively
        // in our work we don't consider the storage cost, so its just one constraint
        MPConstraint[] eight = new MPConstraint[this.nodes.size()];
        constraint = 0;
        for (SensorNode sn : this.nodes) {
            eight[constraint] = solver.makeConstraint(-infinity, sn.getEnergy());
            for (SensorNode snp : this.nodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                eight[constraint].setCoefficient(x[snp.getUuid()][sn.getUuid()], sn.calculateReceivingCost());
                eight[constraint].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], sn.calculateReceivingCost());

                eight[constraint].setCoefficient(x[sn.getUuid() + n][snp.getUuid()],
                        sn.calculateTransmissionCost(snp));
                eight[constraint]
                        .setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], sn.calculateTransmissionCost(snp));

            }
            constraint++;
        }

        // set Objective, (maximize flow from source to data in nodes)
        MPObjective objective = solver.objective();
        for (DataNode dn : this.dNodes) {
            objective.setCoefficient(x[0][dn.getUuid()], 1);
        }
        objective.setMaximization();

        // solve
        final MPSolver.ResultStatus resultStatus = solver.solve();

        // if objective value equals the total number of data packets to be offloaded,
        // then problem is feasible
        System.out.println(
                "Objective: " + objective.value() + " data packet count: " + this.dNodes.size() * dataPacketCount);
        if (objective.value() == this.dNodes.size() * dataPacketCount) {
            return true;
        }

        return false;

    }

    private void makeMaxFlowEdges(MPVariable[][] x, MPSolver solver) {

        // make all edges from the source node to every node that is NOT a data in-node
        // capacity 0
        int n = this.nodes.size();
        int sink = 2 * n + 1;
        MPConstraint c = solver.makeConstraint(0.0, 0.0, "no Edge from i to j");
        for (SensorNode sn : this.nodes) {
            if (!(sn instanceof DataNode)) {
                // storage in-node
                c.setCoefficient(x[0][sn.getUuid()], 1);
                // storage out-node
                c.setCoefficient(x[0][sn.getUuid() + n], 1);
            } else {
                // data out-node
                c.setCoefficient(x[0][sn.getUuid() + n], 1);

            }
        }
        // sink node
        c.setCoefficient(x[0][sink], 1);

        // make all edges from the data in-nodes to every node that is NOT its
        // respective out-node capacity 0

        for (DataNode dn : this.dNodes) {

            // source node
            c.setCoefficient(x[dn.getUuid()][0], 1);

            // other data in-nodes
            for (DataNode dnp : this.dNodes) {
                if (dnp.getUuid() == dn.getUuid()) {
                    continue;
                }
                c.setCoefficient(x[dn.getUuid()][dnp.getUuid()], 1);
                // all other data out-nodes
                c.setCoefficient(x[dn.getUuid()][dnp.getUuid() + n], 1);
            }

            // storage nodes
            for (StorageNode sn : this.sNodes) {
                // in-node
                c.setCoefficient(x[dn.getUuid()][sn.getUuid()], 1);
                // out-node
                c.setCoefficient(x[dn.getUuid()][sn.getUuid() + n], 1);
            }
            // sink node
            c.setCoefficient(x[dn.getUuid()][sink], 1);
        }

        // make all edges from data out-node to:
        // source capacity 0 (1)
        // its respective in-node capacity 0 (2)
        // any data in-node it is NOT directly connected to capacity 0 (3)
        // any other data out-node capacity 0 (4)
        // any storage in- node it is not directly connected to capacity 0 (5)
        // every storage out-node capacity 0 (6)
        // sink capacity 0 (7)
        for (DataNode dn : this.dNodes) {
            // (1)
            c.setCoefficient(x[dn.getUuid() + n][0], 1);
            // (2)
            c.setCoefficient(x[dn.getUuid() + n][dn.getUuid()], 1);
            for (DataNode dnp : this.dNodes) {
                if (dn.getUuid() == dnp.getUuid()) {
                    continue;
                }
                if (!isConnected(dnp, dn)) {
                    // (3)
                    c.setCoefficient(x[dn.getUuid() + n][dnp.getUuid()], 1);
                }
                // (4)
                c.setCoefficient(x[dn.getUuid() + n][dnp.getUuid() + n], 1);
            }
            for (StorageNode sn : this.sNodes) {
                if (!isConnected(sn, dn)) {
                    // (5)
                    c.setCoefficient(x[dn.getUuid() + n][sn.getUuid()], 1);
                }
                // (6)
                c.setCoefficient(x[dn.getUuid() + n][sn.getUuid() + n], 1);
            }
            // (7)
            c.setCoefficient(x[dn.getUuid() + n][sink], 1);
        }

        // make all edges from storage in-nodes to any other node that is not its
        // respective out-node capacity 0
        for (StorageNode sn : this.sNodes) {
            // source node
            c.setCoefficient(x[sn.getUuid()][0], 1);

            for (DataNode dn : this.dNodes) {
                // data nodes
                // in-nodes
                c.setCoefficient(x[sn.getUuid()][dn.getUuid()], 1);
                // out-nodes
                c.setCoefficient(x[sn.getUuid()][dn.getUuid() + n], 1);
            }

            for (StorageNode snp : this.sNodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                // other storage in-nodes
                c.setCoefficient(x[sn.getUuid()][snp.getUuid()], 1);
                // all other storage out-nodes
                c.setCoefficient(x[sn.getUuid()][snp.getUuid() + n], 1);
            }
            // sink
            c.setCoefficient(x[sn.getUuid()][sink], 1);
        }
        // make all edges from storage out-nodes to
        // source node capacity 0
        // data in-nodes that it is not directly connected to capacity 0
        // data out-nodes capacity 0
        // its respective storage in-node capcity 0
        // all other storage in-nodes it is not connected to capacity 0
        // all other other storage out-nodes capacity 0
        for (StorageNode sn : this.sNodes) {
            // source
            c.setCoefficient(x[sn.getUuid() + n][0], 1);
            for (DataNode dn : this.dNodes) {
                if (!isConnected(dn, sn)) {
                    // data in-nodes it is not connected to directly
                    c.setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);
                }
                // data out-nodes
                c.setCoefficient(x[sn.getUuid() + n][dn.getUuid() + n], 1);
            }
            // its respective in-node
            c.setCoefficient(x[sn.getUuid() + n][sn.getUuid()], 1);

            for (StorageNode snp : this.sNodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                if (!isConnected(sn, snp)) {
                    // storage in-node it is not directly connected to
                    c.setCoefficient(x[sn.getUuid() + n][snp.getUuid()], 1);
                }
                // all other storage out-nodes
                c.setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], 1);
            }
        }
        // make all edges from the sink node to any other node capacity 0
        // source
        c.setCoefficient(x[sink][0], 1);
        // sensor in and out-nodes
        for (SensorNode sn : this.nodes) {
            c.setCoefficient(x[sink][sn.getUuid()], 1);
            c.setCoefficient(x[sink][sn.getUuid() + n], 1);
        }
        // finally if i==j, x[i][j]=0
        for (int i = 0; i < x.length; i++) {
            c.setCoefficient(x[i][i], 1);
        }

    }

    /**
     * Saves the network in the <b>DIMAC</b> format
     * that can be used for the min-cost flow program
     * <a href="https://github.com/iveney/cs2">CS2</a>.
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
        return from.calculateTransmissionCost(to) + to.calculateReceivingCost();
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

    public void setBatteryCapacity(int batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
        for (SensorNode n : this.nodes) {
            n.setBatteryCapacity(batteryCapacity);
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
                            sn.getName(), sn.getSpaceLeft(), this.storageCapacity));
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
