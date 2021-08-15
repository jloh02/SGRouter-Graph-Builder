package com.jonathan.sgrouter.graphbuilder.builders.geotools;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShpNode implements Comparable<ShpNode> {
  String id, name;
  double lat, lon;

  public ShpNode(ShpNode n) {
    this.id = n.getId();
    this.name = n.getName();
    this.lat = n.getLat();
    this.lon = n.getLon();
  }

  @Override
  public int compareTo(ShpNode o) {
    String idA = this.id;
    String idB = o.id;
    if (idA.substring(0, 2).equals(idB.substring(0, 2)))
      return Integer.parseInt(idA.substring(2)) - Integer.parseInt(idB.substring(2));
    return idA.compareTo(idB);
  }
}
