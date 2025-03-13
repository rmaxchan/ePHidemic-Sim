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
        mapImage = new Image(getClass().getResource("/com/epidemicsim/phmap.png").toExternalForm());
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
            int x = random.nextInt(WIDTH);
            int y = (i < PEOPLE_COUNT / 3) ? random.nextInt(HEIGHT / 3) :
                    (i < 2 * PEOPLE_COUNT / 3) ? random.nextInt(HEIGHT / 3) + HEIGHT / 3 :
                            random.nextInt(HEIGHT / 3) + 2 * HEIGHT / 3;
            people.add(new Person(x, y));
        }
        people.get(0).infected = true; //start infection
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
        gc.strokeLine(0, HEIGHT / 3, WIDTH, HEIGHT / 3);
        gc.strokeLine(0, 2 * HEIGHT / 3, WIDTH, 2 * HEIGHT / 3);

        for (Person p : people) {
            if (p.infected) {
                p.infectionTime++;
                if (p.infectionTime > RECOVERY_TIME) p.infected = false; // Recover
            }
            spreadInfection(p);
            gc.setFill(p.infected ? Color.RED : Color.GREEN);
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
