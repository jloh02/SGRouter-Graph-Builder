package com.jonathan.sgrouter.graphbuilder.builders.datamall;

public class BusStop {
  public String description;
  public double lat, lon;

  public BusStop(String description, double lat, double lon) {
    this.description = description;
    this.lat = lat;
    this.lon = lon;
  }

  @Override
  public String toString() {
    return String.format("%s %f %f", description, lat, lon);
  }
}
