package com.jonathan.sgrouter.graphbuilder.builders.gmap;

import com.jonathan.sgrouter.graphbuilder.models.config.DefaultTiming;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class GmapTiming {
  public double speed, stopTime;

  public GmapTiming(DefaultTiming dt) {
    this.speed = dt.getDefaultSpeed();
    this.stopTime = dt.getDefaultStopTime();
  }

  public boolean equals(GmapTiming b) {
    return speed == b.speed && stopTime == b.stopTime;
  }
}
