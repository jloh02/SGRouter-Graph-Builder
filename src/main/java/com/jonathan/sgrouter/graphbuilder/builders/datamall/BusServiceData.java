package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BusServiceData extends DatamallConn<BusServiceKey, BusService> {
  @Override
  String initDatamallType() {
    return "BusServices";
  }

  @Override
  void processData(JsonArray value) {
    for (int i = 0; i < value.size(); i++) {
      JsonObject x = value.get(i).getAsJsonObject();
      output.put(
          new BusServiceKey(x.get("ServiceNo").getAsString(), x.get("Direction").getAsInt()),
          new BusService(
              new String[] {
                optString(x.get("AM_Offpeak_Freq")),
                optString(x.get("PM_Offpeak_Freq")),
                optString(x.get("AM_Peak_Freq")),
                optString(x.get("PM_Peak_Freq"))
              }));
    }
  }
}
