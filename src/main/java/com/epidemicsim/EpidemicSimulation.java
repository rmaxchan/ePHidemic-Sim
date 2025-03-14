package com.epidemicsim;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EpidemicSimulation extends Application {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int PEOPLE_COUNT = 100;
    private static final int INFECTION_RADIUS = 10;
    private static final double INFECTION_RATE = 0.2;
    private static final int RECOVERY_TIME = 500;

    private Timeline timeline;
    private List<Person> people = new ArrayList<>();
    private Random random = new Random();
    private Image mapImage;

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        mapImage = new Image(getClass().getResource("/com/epidemicsim/phmap3.png").toExternalForm());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Button startButton = new Button("Start Simulation");
        startButton.setOnAction(e -> startSimulation(gc));

        VBox root = new VBox(startButton, canvas);
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Epidemic Simulation - Philippines");
        primaryStage.show();

        initializePeople();
    }

    private void initializePeople() {
        for (int i = 0; i < PEOPLE_COUNT; i++) {
            int x, y;
            double region = random.nextDouble();
            if (region < 0.4) { // Luzon
                x = random.nextInt(((WIDTH / 3) + 20), (WIDTH * 2 / 3 - 50));
                y = random.nextInt(20,(HEIGHT / 3) + 100);
            } else if (region < 0.7) { // Visayas
                x = random.nextInt(((WIDTH / 3) + 100), ((WIDTH * 2 / 3)) + 30);
                y = random.nextInt(((HEIGHT / 3) + 100), (2 * HEIGHT / 3));
            } else { // Mindanao
                x = random.nextInt(((WIDTH / 3) + 50), ((WIDTH * 2 / 3)) + 60);
                y = random.nextInt((2 * HEIGHT / 3), HEIGHT - 50);
            }
            people.add(new Person(x, y));
        }
        people.get(0).infected = true; //start infection
    }

    private void angledStrokeLine(GraphicsContext gc, double x, double y, double width, double angle) {
        double x2 = x + width * Math.cos(Math.toRadians(angle));
        double y2 = y + width * Math.sin(Math.toRadians(angle));
        gc.strokeLine(x, y, x2, y2);
    }

    private void startSimulation(GraphicsContext gc) {
        timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> update(gc)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void update(GraphicsContext gc) {
        gc.clearRect(0, 0, WIDTH, HEIGHT);
        gc.drawImage(mapImage, 0, 0, WIDTH, HEIGHT);
        gc.setStroke(Color.BLACK);
        angledStrokeLine(gc, 0, (HEIGHT / 3) + 300, WIDTH * 2, -28); // topline
        angledStrokeLine(gc, 0, (2 * HEIGHT / 3) + 253, WIDTH * 2, -33); // bottomline

        for (Person p : people) {
            if (p.infected) {
                p.infectionTime++;
                if (p.infectionTime > RECOVERY_TIME) p.infected = false; // Recover
            }
            spreadInfection(p);
            gc.setFill(p.infected ? Color.RED : Color.BLUE);
            gc.fillOval(p.x, p.y, 5, 5);
        }
    }

    private void spreadInfection(Person person) {
        if (!person.infected) return;
        for (Person other : people) {
            if (!other.infected && random.nextDouble() < INFECTION_RATE && distance(person, other) < INFECTION_RADIUS) {
                other.infected = true;
            }
        }
    }

    private double distance(Person a, Person b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private static class Person {
        int x, y;
        boolean infected = false;
        int infectionTime = 0;

        Person(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
