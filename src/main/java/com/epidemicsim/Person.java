package com.epidemicsim;

public class Person {
    double x, y;
    double angle = Math.random() * Math.PI * 2;
    int region;
    EpidemicSimulation.State state = EpidemicSimulation.State.SUSCEPTIBLE;
    int timer = 0; // time/ticks spent in the current disease stage

    Person(int region) {
        this.region = region;
    }
}
