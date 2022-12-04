import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.util.Set;

public class SensorNetworkGraph extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final double X_SCALE;
    private final double Y_SCALE;

    public SensorNetworkGraph(Network network, double width, double height) {
        this.setWidth(width);
        this.setHeight(height);

        this.X_SCALE = network.getWidth() / 10;
        this.Y_SCALE = network.getLength() / 10;

        this.canvas = new Canvas(width - 40, height - 40);
        this.gc = this.canvas.getGraphicsContext2D();
        this.drawAxis(network);
        this.drawNetwork(network);
        this.getChildren().add(this.canvas);
    }

    public void drawAxis(Network network) {

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

    public void drawNetwork(Network network) {
        this.gc.beginPath();
        this.gc.setStroke(Color.SKYBLUE);

        SensorNode n1;
        for (Map.Entry<SensorNode, Set<SensorNode>> entry : network.getAdjacencyLists().entrySet()) {
            n1 = entry.getKey();
            for (SensorNode n2 : entry.getValue()) {
                this.gc.moveTo(scaleX(n1.getX()), scaleY(n1.getY()));
                this.gc.lineTo(scaleX(n2.getX()), scaleY(n2.getY()));
            }
        }
        this.gc.stroke();

        this.gc.setTextAlign(TextAlignment.CENTER);
        this.gc.setTextBaseline(VPos.CENTER);

        double x, y;
        for (SensorNode node : network.getSensorNodes()) {
            if (node instanceof GeneratorNode) {
                this.gc.setStroke(Color.RED);
            } else {
                this.gc.setStroke(Color.GREEN);
            }
            x = scaleX(node.getX());
            y = scaleY(node.getY());
            this.gc.strokeOval(x - 4, y - 4, 8, 8);
            this.gc.fillText(node.getName(), x, y - 10);
        }
        this.gc.closePath();
    }

    private double scaleX(double x) {
        return this.pointToScale(x, this.X_SCALE);
    }

    private double scaleY(double y) {
        return this.canvas.getHeight() - this.pointToScale(y, this.Y_SCALE);
    }

    private double pointToScale(double val, double scale) {
//        Pixels -> Real
//        return ((val - 40) / 50) * increment;
        return (50 * val) / scale + 40;
    }
}