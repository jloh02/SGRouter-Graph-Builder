package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BusStopData extends DatamallConn<String, BusStop> {
  @Override
  String initDatamallType() {
    return "BusStops";
  }

  @Override
  void processData(JsonArray value) {
    for (int i = 0; i < value.size(); i++) {
      JsonObject x = value.get(i).getAsJsonObject();
      output.put(
          x.get("BusStopCode").getAsString(),
          new BusStop(
              x.get("Description").getAsString(),
              x.get("Latitude").getAsDouble(),
              x.get("Longitude").getAsDouble()));
    }
  }
}
