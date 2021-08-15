package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TrafficSpeed extends DatamallConn<Integer, TrafficSpeedBand> {
  int count;

  @Override
  String initDatamallType() {
    count = 0;
    return "TrafficSpeedBandsv2";
  }

  @Override
  void processData(JsonArray value) {
    for (int i = 0; i < value.size(); i++) {
      JsonObject x = value.get(i).getAsJsonObject();
      try {
        output.put(
            count,
            new TrafficSpeedBand(
                optString(x.get("Location")),
                optDouble(x.get("MinimumSpeed")),
                optDouble(x.get("MaximumSpeed"))));
      } catch (NumberFormatException e) {
      } // null speeds
      count++;
    }
  }
}
