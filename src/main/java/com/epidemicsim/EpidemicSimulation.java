package com.epidemicsim;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;

public class EpidemicSimulation extends Application {

    //WIP
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int CHART_HEIGHT = 150;

    //ticks
    private static final int TICK_MS = 50;
    private static final int TICKS_PER_DAY = 10;
    private static final int INCUBATION_TICKS = 30;
    private static final int INFECTIOUS_TICKS = 140;
    private static final double MAX_SPEED = 1.5;

    // states for dots -- WIP -- to change according to map colorized regions (with high contrast for visibility)
    public enum State { SUSCEPTIBLE, EXPOSED, INFECTED, RECOVERED, DEAD }
    private static final Color[] STATE_COLORS = {
            Color.DODGERBLUE, Color.ORANGE, Color.RED, Color.LIMEGREEN, Color.DIMGRAY
    };
    private static final String[] STATE_NAMES = {
            "Susceptible", "Exposed", "Infected", "Recovered/Immune", "Dead"
    };

    //
    private static final double[][] REGIONS = {
            {WIDTH / 3.0 + 20, WIDTH * 2.0 / 3 - 50, 20, HEIGHT / 3.0 + 100},                 // Luzon
            {WIDTH / 3.0 + 100, WIDTH * 2.0 / 3 + 30, HEIGHT / 3.0 + 100, 2.0 * HEIGHT / 3},  // Visayas
            {WIDTH / 3.0 + 50, WIDTH * 2.0 / 3 + 60, 2.0 * HEIGHT / 3, HEIGHT - 50}           // Mindanao
    };
    private static final double[] REGION_SHARE = {0.4, 0.3, 0.3};
    private static final String[] REGION_NAMES = {"Luzon", "Visayas", "Mindanao"};

    /*
    private void angledStrokeLine(GraphicsContext gc, double x, double y, double width, double angle) {
        double x2 = x + width * Math.cos(Math.toRadians(angle));
        double y2 = y + width * Math.sin(Math.toRadians(angle));
        gc.strokeLine(x, y, x2, y2);
    }
    */


    // Adjustable sliders
    private double transmission = 0.08;   // chance per tick of catching it from one nearby infected person
    private double infectionRadius = 8;
    private double mobility = 1.0;        // 0 = full lockdown, 1 = normal movement
    private double travelChance = 0.002;  // per-tick chance of hopping to another island group
    private double mortality = 0.02;      // chance of dying at the end of the infectious period
    private double vaccinated = 0.0;      // fraction of population likely immune at the start
    private int population = 300;

    //Runtime
    private final Random random = new Random();
    private final List<Person> people = new ArrayList<>();
    private final List<int[]> history = new ArrayList<>();
    private Timeline timeline;
    private Image mapImage;
    private GraphicsContext mapGc;
    private GraphicsContext chartGc;
    private Label statsLabel;
    private Button startButton;
    private int tick = 0;
    private int peakInfected = 0;
    private int peakDay = 0;
    private boolean outbreakOver = false;

    @Override
    public void start(Stage primaryStage) {
        Canvas mapCanvas = new Canvas(WIDTH, HEIGHT);
        Canvas chartCanvas = new Canvas(WIDTH, CHART_HEIGHT);
        mapGc = mapCanvas.getGraphicsContext2D();
        chartGc = chartCanvas.getGraphicsContext2D();
        mapImage = loadMap();

        startButton = new Button("Start");
        startButton.setOnAction(e -> toggleRunning());
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> reset());

        GridPane controls = new GridPane();
        controls.setHgap(10);
        controls.setVgap(4);
        int row = 0;
        addSlider(controls, row++, "Transmission", 0.01, 0.4, transmission, v -> transmission = v);
        addSlider(controls, row++, "Infection radius", 3, 25, infectionRadius, v -> infectionRadius = v);
        addSlider(controls, row++, "Mobility (lockdown <-> free)", 0, 1, mobility, v -> mobility = v);
        addSlider(controls, row++, "Inter-island travel", 0, 0.02, travelChance, v -> travelChance = v);
        addSlider(controls, row++, "Mortality", 0, 0.2, mortality, v -> mortality = v);
        addSlider(controls, row++, "Vaccinated at reset", 0, 0.9, vaccinated, v -> vaccinated = v);
        addSlider(controls, row++, "Population at reset", 50, 1000, population, v -> population = (int) Math.round(v));

        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-family: monospace;");

        VBox root = new VBox(8, new HBox(8, startButton, resetButton), controls, mapCanvas, chartCanvas, statsLabel);
        root.setPadding(new Insets(10));

        timeline = new Timeline(new KeyFrame(Duration.millis(TICK_MS), e -> update()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Epidemic Simulation - Philippines");
        primaryStage.show();
        reset();
    }

    private Image loadMap() {
        var url = getClass().getResource("/com/epidemicsim/phmap.png");
        return url == null ? null : new Image(url.toExternalForm());
    }

    private void addSlider(GridPane grid, int row, String name, double min, double max,
                           double value, DoubleConsumer onChange) {
        Slider slider = new Slider(min, max, value);
        slider.setPrefWidth(300);
        Label valueLabel = new Label(format(value));
        slider.valueProperty().addListener((obs, old, v) -> {
            onChange.accept(v.doubleValue());
            valueLabel.setText(format(v.doubleValue()));
        });
        grid.add(new Label(name), 0, row);
        grid.add(slider, 1, row);
        grid.add(valueLabel, 2, row);
    }

    private String format(double v) {
        return Math.abs(v) >= 10 ? String.format("%.0f", v) : String.format("%.3f", v);
    }


    // Simulation methods


    private void toggleRunning() {
        if (outbreakOver) return;
        if (timeline.getStatus() == Timeline.Status.RUNNING) {
            timeline.pause();
            startButton.setText("Resume");
        } else {
            timeline.play();
            startButton.setText("Pause");
        }
    }

    private void reset() {
        timeline.stop();
        startButton.setText("Start");
        people.clear();
        history.clear();
        tick = 0;
        peakInfected = 0;
        peakDay = 0;
        outbreakOver = false;

        for (int i = 0; i < population; i++) {
            int region = pickRegion();
            Person p = new Person(region);
            placeInRegion(p, region);
            if (random.nextDouble() < vaccinated) p.state = State.RECOVERED; // immune
            people.add(p);
        }

        // pick patient zero
        for (Person p : people) {
            if (p.state == State.SUSCEPTIBLE) {
                p.state = State.INFECTED;
                break;
            }
        }

        record();
        draw();
    }

    private int pickRegion() {
        double r = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < REGION_SHARE.length; i++) {
            cumulative += REGION_SHARE[i];
            if (r < cumulative) return i;
        }
        return REGION_SHARE.length - 1;
    }

    private void placeInRegion(Person p, int region) {
        double[] b = REGIONS[region];
        p.region = region;
        p.x = b[0] + random.nextDouble() * (b[1] - b[0]);
        p.y = b[2] + random.nextDouble() * (b[3] - b[2]);
    }

    private void update() {
        tick++;
        movePeople();
        spreadInfection();
        progressDisease();
        record();
        draw();

        int[] c = history.getLast();
        if (c[State.INFECTED.ordinal()] + c[State.EXPOSED.ordinal()] == 0) {
            outbreakOver = true;
            timeline.stop();
            startButton.setText("Outbreak over");
            draw();
        }
    }

    private void movePeople() {
        double speed = MAX_SPEED * mobility;
        for (Person p : people) {
            if (p.state == State.DEAD) continue;

            if (p.state != State.INFECTED && random.nextDouble() < travelChance * mobility) {
                int dest = random.nextInt(REGIONS.length);
                if (dest != p.region) placeInRegion(p, dest);
            }

            p.angle += random.nextGaussian() * 0.4;
            p.x += Math.cos(p.angle) * speed;
            p.y += Math.sin(p.angle) * speed;


            double[] b = REGIONS[p.region];
            if (p.x < b[0] || p.x > b[1]) {
                p.x = Math.max(b[0], Math.min(b[1], p.x));
                p.angle = Math.PI - p.angle;
            }
            if (p.y < b[2] || p.y > b[3]) {
                p.y = Math.max(b[2], Math.min(b[3], p.y));
                p.angle = -p.angle;
            }
        }
    }

    private void spreadInfection() {
        double r2 = infectionRadius * infectionRadius;
        for (Person source : people) {
            if (source.state != State.INFECTED) continue;
            for (Person other : people) {
                if (other.state != State.SUSCEPTIBLE) continue;
                double dx = source.x - other.x;
                double dy = source.y - other.y;
                if (dx * dx + dy * dy < r2 && random.nextDouble() < transmission) {
                    other.state = State.EXPOSED;
                    other.timer = 0;
                }
                //update();
            }
        }
    }

    private void progressDisease() {
        for (Person p : people) {
            if (p.state == State.EXPOSED) {
                if (++p.timer >= INCUBATION_TICKS) {
                    p.state = State.INFECTED;
                    p.timer = 0;
                }
            } else if (p.state == State.INFECTED) {
                if (++p.timer >= INFECTIOUS_TICKS) {
                    p.state = random.nextDouble() < mortality ? State.DEAD : State.RECOVERED;
                }
            }
        }
    }

    private void record() {
        int[] counts = new int[State.values().length];
        for (Person p : people) counts[p.state.ordinal()]++;
        history.add(counts);

        int infected = counts[State.INFECTED.ordinal()];
        if (infected > peakInfected) {
            peakInfected = infected;
            peakDay = tick / TICKS_PER_DAY;
        }
    }

    // Display methods

    private void draw() {
        drawMap();
        drawChart();
        drawStats();
    }

    private void drawMap() {
        mapGc.clearRect(0, 0, WIDTH, HEIGHT);

        try {
            if (mapImage == null) {
                throw new IllegalStateException("Map image is missing.");
            }

            mapGc.drawImage(mapImage, 0, 0, WIDTH, HEIGHT);

        } catch (Exception e) {
            mapGc.setFill(Color.LIGHTSTEELBLUE);
            mapGc.fillRect(0, 0, WIDTH, HEIGHT);
            System.err.println("Failed to load map image: " + e.getMessage());
        }

        for (Person p : people) {
            mapGc.setFill(STATE_COLORS[p.state.ordinal()]);
            double size = p.state == State.DEAD ? 3 : 5;
            mapGc.fillOval(p.x - size / 2, p.y - size / 2, size, size);
        }
    }

    private void drawChart() {
        chartGc.setFill(Color.WHITE);
        chartGc.fillRect(0, 0, WIDTH, CHART_HEIGHT);
        chartGc.setStroke(Color.LIGHTGRAY);
        chartGc.strokeRect(0.5, 0.5, WIDTH - 1, CHART_HEIGHT - 1);
        if (history.size() < 2 || people.isEmpty()) return;

        int n = people.size();
        double xStep = (double) (WIDTH - 1) / (history.size() - 1);
        chartGc.setLineWidth(1.5);
        for (State s : State.values()) {
            chartGc.setStroke(STATE_COLORS[s.ordinal()]);
            chartGc.beginPath();
            for (int i = 0; i < history.size(); i++) {
                double x = i * xStep;
                double y = CHART_HEIGHT - 4 - ((double) history.get(i)[s.ordinal()] / n) * (CHART_HEIGHT - 8);
                if (i == 0) chartGc.moveTo(x, y);
                else chartGc.lineTo(x, y);
            }
            chartGc.stroke();
        }
        chartGc.setLineWidth(1);
    }

    private void drawStats() {
        int[] c = history.getLast();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Day %d%s%n", tick / TICKS_PER_DAY, outbreakOver ? "  (outbreak over)" : ""));
        for (State s : State.values()) {
            sb.append(String.format("%-17s %4d%n", STATE_NAMES[s.ordinal()], c[s.ordinal()]));
        }
        sb.append(String.format("Peak infected: %d (day %d)%n", peakInfected, peakDay));
        sb.append("Infected by region: ");
        for (int r = 0; r < REGIONS.length; r++) {
            int count = 0;
            for (Person p : people) if (p.region == r && p.state == State.INFECTED) count++;
            sb.append(REGION_NAMES[r]).append(' ').append(count).append("  ");
        }
        statsLabel.setText(sb.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
