package com.jonathan.sgrouter.graphbuilder.models;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.linearref.LocationIndexedLine;

public class IndexedBusStopRoad extends LocationIndexedLine {
  private double speed;

  public IndexedBusStopRoad(Geometry linearGeom, double speed) {
    super(linearGeom);
    this.speed = speed;
  }

  public double getSpeed() {
    return this.speed;
  }
}
