package com.jonathan.sgrouter.graphbuilder.models;

import lombok.Data;

@Data
public class Node {
  String srcKey, name;
  double lat, lon;

  public Node(String key, String name, double lat, double lon) {
    this.srcKey = key;
    this.name = name;
    this.lat = lat;
    this.lon = lon;
  }
}
