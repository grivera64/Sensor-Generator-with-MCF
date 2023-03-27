import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.generator.SensorNetworkGraph;
import com.grivera.generator.sensors.SensorNode;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Scanner;

/**
 * Main class for the program.
 */
public class SensorToFlowNetworkMain extends Application {
    public static final Scanner keyboard = new Scanner(System.in);
    public static final double guiWidth = 640;
    public static final double guiHeight = 640;
    public static SensorNetworkGraph guiGraph;

    /**
     * The entry point of the application.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Network network;

        System.out.println("Please enter an option (F)ile/(G)enerate/(Q)uit:");
        System.out.print("(Q) > ");
        int option = keyboard.nextLine().charAt(0);

        switch (option) {
            case 'F', 'f' -> network = readNetwork();
            case 'G', 'g' -> {
                network = generateNetwork();
                network.save("sensor_network.sn");
            }
            default -> {
                System.out.println("Thank you for using Sensor-Generator-with-MCF!");
                System.exit(0);
                return;
            }
        }

        prettyPrint(network.getDataNodes(), "Generator Nodes   Coordinates");
        prettyPrint(network.getStorageNodes(), "Storage Nodes    Coordinates");

        System.out.printf("Network is connected: %b\n", network.isConnected());
        System.out.printf("Network is feasible: %b\n", network.isFeasible());
        System.out.printf("Network is feasible (Max Flow): %b\n", network.isMaxFlowFeasible());
        System.out.printf("MCF total cost: %d micro J\n", network.getMinCostIlp());

        network.saveAsCsInp("output_sensor_flow_diagram.inp");
        guiGraph = new SensorNetworkGraph(network, guiWidth, guiHeight);

        Thread t = new Thread(() -> highlightPath(network));
        t.start();
        launch(args);
        System.exit(0);
    }

    /**
     * Asks for com.grivera.generator.Network parameters input through stdin and generates a com.grivera.generator.Network object.
     * @return the generated network
     * @see Network
     */
    public static Network generateNetwork() {
        System.out.println("Please enter the width (x) of the sensor network:");
        System.out.print("x = ");
        double width = keyboard.nextDouble();
        keyboard.nextLine();

        System.out.println("Please enter the height (y) of the sensor network: ");
        System.out.print("y = ");
        double height = keyboard.nextDouble();
        keyboard.nextLine();

        System.out.println("Please enter the number of sensor nodes (N) to generate in the sensor network:");
        System.out.print("N = ");
        int nodeCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the number the transmission range (Tr) in meters:");
        System.out.print("Tr = ");
        double transmissionRange = keyboard.nextDouble();
        keyboard.nextLine();

        System.out.println("Please enter the number of Data Nodes (p) to generate:");
        System.out.print("p = ");
        int gNodeCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the number of data packets (q) each Data Node has:");
        System.out.print("q = ");
        int packetsCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the amount of packets (m) each Storage Node has:");
        System.out.print("m = ");
        int storageCount = keyboard.nextInt();
        keyboard.nextLine();

        System.out.println("Please enter the battery capacity (c) each Sensor Node has:");
        System.out.print("c = ");
        int energyCapacity = keyboard.nextInt();
        keyboard.nextLine();
        System.out.println();

        return SensorNetwork.of(width, height, nodeCount, transmissionRange, gNodeCount, packetsCount, storageCount, energyCapacity);
    }

    /**
     * Asks for an .sn file through stdin and generates a com.grivera.generator.Network object.
     * @return the generated network
     * @see Network
     */
    public static Network readNetwork() {
        System.out.println("Please enter a file name:");
        System.out.print("> ");
        String fileName = keyboard.nextLine().trim();

        return SensorNetwork.from(fileName);
    }

    /**
     * Pretty Prints out a list table with the provided title.
     * @param list the list of elements to print
     * @param title the title of the table
     */
    public static void prettyPrint(List<?> list, String title) {
        System.out.println(title);
        System.out.println("=================================");
        for (Object o : list) {
            System.out.println(o);
        }
        System.out.println();
    }

    /**
     * Asks for a source and destination node to draw
     * an orange path outline on the com.grivera.generator.Network graph's GUI.
     * @param network the network to draw on
     * @see Network
     */
    private static void highlightPath(Network network) {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        String command;
        SensorNode n1 = null;
        SensorNode n2 = null;
        while (true) {
            while (true) {
                System.out.print("Please enter the Data node to traverse from (Q to quit/C to clear):\n> ");
                command = keyboard.nextLine().trim();

                if (command.matches("(?:[Cc][Ll][Ee][Aa][Rr]|[Cc])")) {
                    System.out.println("Highlight reset!");
                    guiGraph.resetHighlight();
                    continue;
                }
                else if (command.matches("(?:[Qq][Uu][Ii][Tt]|[Qq])")) {
                    System.out.println("Quitting...");
                    System.exit(0);
                }
                else if (command.matches("^\\d+")) {
                    n1 = network.getDataNodes().get(Integer.parseInt(command) - 1);
                } else if (command.matches("DN\\d+")) {
                    n1 = network.getDataNodes().get(Integer.parseInt(command.substring(2)) - 1);
                } else {
                    System.out.println("Invalid input! Please try again...\n");
                    continue;
                }
                System.out.printf("Selected: %s\n", n1);
                break;
            }

            while (true) {
                System.out.print("Please enter the Sensor node to traverse to (Q to quit):\n> ");
                command = keyboard.nextLine().trim();

                if (command.matches("(?:[Qq][Uu][Ii][Tt]|[Qq])")) {
                    System.out.println("Quitting...");
                    System.exit(0);
                } else if (command.matches("^\\d+")) {
                    n2 = network.getStorageNodes().get(Integer.parseInt(command) - 1);
                } else if (command.matches("SN\\d+")) {
                    n2 = network.getStorageNodes().get(Integer.parseInt(command.substring(2)) - 1);
                } else {
                    System.out.println("Invalid input! Please try again...\n");
                    continue;
                }
                System.out.printf("Selected: %s\n", n2);
                break;
            }

            guiGraph.resetHighlight();
            guiGraph.highlightPath(n1, n2);
        }
    }

    /**
     * The entry point for a JavaFX application.
     * @param primaryStage the stage of the JavaFX application
     * @see Stage
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Wireless Sensor Network Generator | Giovanni Rivera");
        primaryStage.setWidth(guiWidth);
        primaryStage.setHeight(guiHeight);
        primaryStage.setScene(new Scene(guiGraph));
        primaryStage.setResizable(false);
        primaryStage.show();
        guiGraph.saveAsPng("sensor_network.png");
    }
}