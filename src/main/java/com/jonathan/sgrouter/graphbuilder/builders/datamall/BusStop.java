package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class BusStop {
  public String description;
  public double lat, lon;
}
