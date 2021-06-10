package com.jonathan.sgrouter.graphbuilder.builders.gmap;

import com.jonathan.sgrouter.graphbuilder.models.config.DefaultTiming;

public class GmapTiming {
  public double speed, stopTime;

  public GmapTiming(double speed, double stopTime) {
    this.speed = speed;
    this.stopTime = stopTime;
  }

  public GmapTiming(DefaultTiming dt) {
    this.speed = dt.getDefaultSpeed();
    this.stopTime = dt.getDefaultStopTime();
  }

  public boolean equals(GmapTiming b) {
    return speed == b.speed && stopTime == b.stopTime;
  }

  @Override
  public String toString() {
    return String.format("{speed=%f, stopTime=%f}", speed, stopTime);
  }
}
