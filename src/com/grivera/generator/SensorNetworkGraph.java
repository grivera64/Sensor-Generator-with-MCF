package com.grivera.generator;

import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the Sensor com.grivera.generator.Network graph pane on the main
 * stage.
 * 
 * @see Pane javafx.scene.layout.Pane
 */
public class SensorNetworkGraph extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Network network;
    private final double X_SCALE;
    private final double Y_SCALE;
    private boolean isHighlighted;

    /**
     * Creates the pane with the network on it.
     * 
     * @param network
     * @param width
     * @param height
     */
    public SensorNetworkGraph(Network network, double width, double height) {
        this.network = network;
        this.setWidth(width);
        this.setHeight(height);

        this.X_SCALE = network.getWidth() / 10;
        this.Y_SCALE = network.getLength() / 10;

        this.canvas = new Canvas(width - 40, height - 40);
        this.gc = this.canvas.getGraphicsContext2D();
        this.drawAxis();
        this.drawNetwork(network);
        this.getChildren().add(this.canvas);
    }

    private void drawAxis() {

        this.gc.beginPath();
        this.gc.setStroke(Color.BLACK);
        this.gc.setLineWidth(5);

        /* Y-axis */
        this.gc.moveTo(40, 40);
        this.gc.lineTo(40, this.canvas.getHeight() - 40);

        /* X-axis */
        this.gc.moveTo(40, this.canvas.getHeight() - 40);
        this.gc.lineTo(this.canvas.getWidth() - 40, this.canvas.getHeight() - 40);

        /* Draw x- and y-axis */
        this.gc.stroke();
        this.gc.closePath();

        this.gc.beginPath();
        this.gc.setLineWidth(1);
        this.gc.setStroke(Color.GRAY);
        this.gc.setTextAlign(TextAlignment.CENTER);
        this.gc.setTextBaseline(VPos.CENTER);

        double increment = 0.0;
        for (int i = 40; i < this.canvas.getWidth() - 40; i += 50) {
            this.gc.fillText(String.format("%.2f", increment), i, this.canvas.getHeight() - 20);
            increment += this.X_SCALE;

            if (i == 40) {
                continue;
            }
            this.gc.moveTo(i, 40);
            this.gc.lineTo(i, this.canvas.getHeight() - 40);
        }

        increment = 0.0;
        for (int i = 40; i < this.canvas.getWidth() - 40; i += 50) {

            this.gc.fillText(String.format("%.2f", increment), 20, this.canvas.getHeight() - i);
            increment += this.Y_SCALE;

            if (i == 40) {
                continue;
            }
            this.gc.moveTo(40, this.canvas.getHeight() - i);
            this.gc.lineTo(this.canvas.getWidth() - 40, this.canvas.getHeight() - i);
        }

        /* Draw */
        this.gc.stroke();
        this.gc.closePath();
    }

    private void drawNetwork(Network network) {
        this.gc.beginPath();
        this.gc.setStroke(Color.SKYBLUE);

        SensorNode n1;
        for (Map.Entry<SensorNode, Set<SensorNode>> entry : network.getAdjacencyList().entrySet()) {
            n1 = entry.getKey();
            for (SensorNode n2 : entry.getValue()) {
                this.drawLine(n1, n2);
            }
        }
        this.gc.stroke();

        this.gc.setTextAlign(TextAlignment.CENTER);
        this.gc.setTextBaseline(VPos.CENTER);

        for (SensorNode node : network.getSensorNodes()) {
            if (node instanceof DataNode) {
                this.gc.setStroke(Color.RED);
            } else {
                this.gc.setStroke(Color.GREEN);
            }
            this.drawNode(node, 8, true);
        }
        this.gc.closePath();
    }

    private void drawNode(SensorNode node, double radius, boolean hasLabel) {
        double x, y;
        x = scaleX(node.getX());
        y = scaleY(node.getY());
        this.gc.strokeOval(x - 4, y - 4, radius, radius);

        if (hasLabel) {
            this.gc.fillText(node.getName(), x, y - (radius * 1.25));
        }
    }

    private void drawLine(SensorNode from, SensorNode to) {
        this.gc.moveTo(scaleX(from.getX()), scaleY(from.getY()));
        this.gc.lineTo(scaleX(to.getX()), scaleY(to.getY()));
    }

    /**
     * Draws an orange path between the from and to Sensor Nodes that represents the
     * min-cost path.
     *
     * @param from the starting Sensor Node
     * @param to   the ending Sensor Node
     */
    public void highlightPath(SensorNode from, SensorNode to) {
        List<SensorNode> path = this.network.getMinCostPath(from, to);
        System.out.printf("Highlighted Min-Cost Path: %s\n",
                String.join(" -> ",
                        path.stream().map(SensorNode::getName).toArray(CharSequence[]::new)));
        System.out.printf("Cost of Path: %d micro J\n", this.network.calculateCostOfPath(path));

        this.gc.beginPath();
        this.gc.setStroke(Color.DARKORANGE);
        this.gc.setLineWidth(2);

        /* Highlight nodes */
        for (SensorNode node : path) {
            drawNode(node, 10, false);
        }

        /* Highlight the connections */
        for (int i = 0; i < path.size() - 1; i++) {
            this.drawLine(path.get(i), path.get(i + 1));
        }
        this.gc.stroke();
        this.gc.closePath();

        this.isHighlighted = true;
    }

    /**
     * Resets the highlighted orange path.
     */
    public void resetHighlight() {
        if (this.isHighlighted) {
            this.gc.clearRect(0, 0, this.getWidth(), this.getHeight());
            this.drawAxis();
            this.drawNetwork(this.network);
        }
        this.isHighlighted = false;
    }

    private double scaleX(double x) {
        return this.pointToScale(x, this.X_SCALE);
    }

    private double scaleY(double y) {
        return this.canvas.getHeight() - this.pointToScale(y, this.Y_SCALE);
    }

    private double pointToScale(double val, double scale) {
        // Pixels -> Real
        // return ((val - 40) / 50) * increment;
        return (50 * val) / scale + 40;
    }

    /**
     * Saves an image of the Pane
     *
     * @param fileName the path to the png file to save to
     */
    public void saveAsPng(String fileName) {
        WritableImage writableImage = new WritableImage((int) this.getWidth() + 20, (int) this.getHeight() + 20);
        this.snapshot(null, writableImage);

        RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
        try {
            ImageIO.write(renderedImage, "png", new File(fileName));
            System.out.printf("Saved sensor network in file \"%s\"\n", fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
