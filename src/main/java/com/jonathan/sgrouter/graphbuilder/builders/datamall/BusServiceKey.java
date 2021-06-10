package com.jonathan.sgrouter.graphbuilder.builders.datamall;

public class BusServiceKey implements Comparable<BusServiceKey> {
  public String src, dest, service;
  public int direction;

  public BusServiceKey(String service, int direction) {
    this.service = service;
    this.direction = direction;
  }

  public static BusServiceKey fromBusRouteKey(BusRouteKey k) {
    return new BusServiceKey(k.service, k.direction);
  }

  public String toString() {
    return String.format("%s (%s)", service, direction);
  }

  // Set default sorter
  @Override
  public int compareTo(final BusServiceKey k2) {
    if (!this.service.equals(k2.service)) return this.service.compareTo(k2.service);
    return this.direction - k2.direction;
  }

  // To use object as Map key
  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }
}
