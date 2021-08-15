package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BusRouteKey implements Comparable<BusRouteKey> {
  public String service;
  public int direction, stopSequence;

  public String toString() {
    return String.format("%s(%s):%s", service, direction, stopSequence);
  }

  // Set default sorter
  @Override
  public int compareTo(final BusRouteKey k2) {
    if (!this.service.equals(k2.service)) return this.service.compareTo(k2.service);
    if (this.direction != k2.direction) return this.direction - k2.direction;
    return this.stopSequence - k2.stopSequence;
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
