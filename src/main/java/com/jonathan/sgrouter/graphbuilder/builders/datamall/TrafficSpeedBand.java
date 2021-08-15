package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrafficSpeedBand {
  String location;
  double minSpeed, maxSpeed;
}
