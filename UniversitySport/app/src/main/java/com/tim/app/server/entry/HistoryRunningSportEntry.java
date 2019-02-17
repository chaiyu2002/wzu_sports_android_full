package com.tim.app.server.entry;


import java.io.Serializable;

/**
 * 运动数据
 */
public class HistoryRunningSportEntry extends HistorySportEntry implements Serializable {
    private static final long serialVersionUID = 6187447685293862071L;
    private int distance;
    private int stepCount;
    private int speed;//速度
    private double stepPerSecond;
    private double distancePerStep;
    private int qualifiedDistance;

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public double getStepPerSecond() {
        return stepPerSecond;
    }

    public void setStepPerSecond(double stepPerSecond) {
        this.stepPerSecond = stepPerSecond;
    }

    public double getDistancePerStep() {
        return distancePerStep;
    }

    public void setDistancePerStep(double distancePerStep) {
        this.distancePerStep = distancePerStep;
    }

    public int getQualifiedDistance() {
        return qualifiedDistance;
    }

    public void setQualifiedDistance(int qualifiedDistance) {
        this.qualifiedDistance = qualifiedDistance;
    }
}
