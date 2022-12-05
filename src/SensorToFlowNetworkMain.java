import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Scanner;

public class SensorToFlowNetworkMain extends Application {
    public static final Scanner keyboard = new Scanner(System.in);

    public static Network network;

    public static void main(String[] args) {

        System.out.println("Please enter an option (F)ile/(G)enerate/(Q)uit:");
        System.out.print("(Q) > ");
        int option = keyboard.nextLine().charAt(0);

        switch (option) {
            case 'F', 'f' -> network = readNetwork();
            case 'G', 'g' -> network = generateNetwork();
            default -> {
                System.out.println("Thank you for using Sensor-Generator-with-MCF!");
                System.exit(0);
            }
        }

        prettyPrint(network.getSensorNodes(), "Sensor Nodes");
        prettyPrint(network.getGeneratorNodes(), "Generator Nodes");
        prettyPrint(network.getStorageNodes(), "Storage Nodes");

        System.out.printf("Network is connected: %b\n", network.isConnected());
        System.out.printf("Network is feasible: %b\n", network.isFeasible());

//        network.saveAsCsInp("output_sensor_flow_diagram", -1, -1);

        launch(args);
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

    @Override
    public void start(Stage primaryStage) {
        final double width = 640;
        final double height = 640;
        primaryStage.setTitle("Wireless Sensor Network Generator | Giovanni Rivera");
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setScene(new Scene(new SensorNetworkGraph(network, width, height)));
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}