package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BusRouteData extends DatamallBus<BusRouteKey, BusRoute> {
  @Override
  String initDatamallType() {
    return "BusRoutes";
  }

  @Override
  void processData(JsonArray value) {
    for (int i = 0; i < value.size(); i++) {
      JsonObject x = value.get(i).getAsJsonObject();
      output.put(
          new BusRouteKey(
              x.get("ServiceNo").getAsString(),
              x.get("Direction").getAsInt(),
              x.get("StopSequence").getAsInt()),
          new BusRoute(
              optString(x.get("BusStopCode")),
              optDouble(x.get("Distance")),
              optString(x.get("WD_FirstBus")),
              optString(x.get("WD_LastBus")),
              optString(x.get("SAT_FirstBus")),
              optString(x.get("SAT_LastBus")),
              optString(x.get("SUN_FirstBus")),
              optString(x.get("SUN_LastBus"))));
    }
  }
}
