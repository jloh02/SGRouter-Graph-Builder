package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class BusRouteData extends DatamallBus<BusRouteKey, BusRoute> {
  @Override
  String initDatamallType() {
    return "BusRoutes";
  }

  @Override
  void processData(JSONArray value) {
    for (int i = 0; i < value.length(); i++) {
      try {
        JSONObject x = value.getJSONObject(i);
        output.put(
            new BusRouteKey(
                x.getString("ServiceNo"), x.getInt("Direction"), x.getInt("StopSequence")),
            new BusRoute(
                x.optString("BusStopCode"),
                x.optDouble("Distance"),
                x.optString("WD_FirstBus"),
                x.optString("WD_LastBus"),
                x.optString("SAT_FirstBus"),
                x.optString("SAT_LastBus"),
                x.optString("SUN_FirstBus"),
                x.optString("SUN_LastBus")));
      } catch (JSONException e) {
        log.error(e.getMessage());
      }
    }
  }
}
