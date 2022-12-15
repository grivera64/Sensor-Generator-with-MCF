import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Scanner;

public class SensorToFlowNetworkMain extends Application {
    public static final Scanner keyboard = new Scanner(System.in);
    public static final double guiWidth = 640;
    public static final double guiHeight = 640;
    public static SensorNetworkGraph guiGraph;

    public static void main(String[] args) {
        Network network;

        System.out.println("Please enter an option (F)ile/(G)enerate/(Q)uit:");
        System.out.print("(Q) > ");
        int option = keyboard.nextLine().charAt(0);

        switch (option) {
            case 'F', 'f' -> network = readNetwork();
            case 'G', 'g' -> network = generateNetwork();
            default -> {
                System.out.println("Thank you for using Sensor-Generator-with-MCF!");
                System.exit(0);
                return;
            }
        }

        prettyPrint(network.getGeneratorNodes(), "Generator Nodes   Coordinates");
        prettyPrint(network.getStorageNodes(), "Storage Nodes    Coordinates");

        System.out.printf("Network is connected: %b\n", network.isConnected());
        System.out.printf("Network is feasible: %b\n", network.isFeasible());

        network.saveAsCsInp("output_sensor_flow_diagram");
        guiGraph = new SensorNetworkGraph(network, guiWidth, guiHeight);

        Thread t = new Thread(() -> highlightPath(network));
        t.start();
        launch(args);
        System.exit(0);
    }

    public static Network generateNetwork() {
        Network network;

        do {
            network = createNetwork();

            if (!network.isConnected()) {
                System.out.println("The Network is not connected! Please try again....");
                System.out.println();
                continue;
            }

            if (!network.isFeasible()) {
                System.out.println("The Network is not feasible! Please try again....");
                System.out.println();
                continue;
            }
            break;
        } while (true);

        return network;
    }

    public static Network createNetwork() {
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

        System.out.println();
        return new SensorNetwork(width, height, nodeCount, transmissionRange, gNodeCount, packetsCount, storageCount);
    }

    public static Network readNetwork() {
        System.out.println("Please enter a file name:");
        System.out.print("> ");
        String fileName = keyboard.nextLine().trim();

        return new SensorNetwork(fileName);
    }

    public static void prettyPrint(List<?> list, String title) {
        System.out.println(title);
        System.out.println("=================================");
        for (Object o : list) {
            System.out.println(o);
        }
        System.out.println();
    }

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
                System.out.print("Please enter the Data node to traverse from (Q to quit):\n> ");
                command = keyboard.nextLine().trim();

                if (command.matches("(?:[Qq][Uu][Ii][Tt]|[Qq])")) {
                    System.out.println("Quitting...");
                    System.exit(0);
                }
                else if (command.matches("^\\d+")) {
                    n1 = network.getGeneratorNodes().get(Integer.parseInt(command) - 1);
                } else if (command.matches("DN\\d+")) {
                    n1 = network.getGeneratorNodes().get(Integer.parseInt(command.substring(3)) - 1);
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
                    n2 = network.getStorageNodes().get(Integer.parseInt(command.substring(3)) - 1);
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