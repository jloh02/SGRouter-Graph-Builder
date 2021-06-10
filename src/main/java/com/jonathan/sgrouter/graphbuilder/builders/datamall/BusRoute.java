package com.jonathan.sgrouter.graphbuilder.builders.datamall;

public class BusRoute {
  public String src, SUN_first, WD_first, SAT_last, SAT_first, WD_last, SUN_last;
  public double distance;

  public BusRoute(
      String src,
      double distance,
      String WD_first,
      String WD_last,
      String SAT_first,
      String SAT_last,
      String SUN_first,
      String SUN_last) {
    this.src = src;
    this.distance = distance;
    this.WD_first = WD_first;
    this.WD_last = WD_last;
    this.SAT_first = SAT_first;
    this.SAT_last = SAT_last;
    this.SUN_first = SUN_first;
    this.SUN_last = SUN_last;
  }

  @Override
  public String toString() {
    return String.format(
        "%s (%f): %s %s %s %s",
        src, distance, WD_first, WD_last, SAT_first, SAT_last, SUN_first, SUN_last);
  }
}
