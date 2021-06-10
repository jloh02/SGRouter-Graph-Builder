package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class BusServiceData extends DatamallBus<BusServiceKey, BusService> {
  @Override
  String initDatamallType() {
    return "BusServices";
  }

  @Override
  void processData(JSONArray value) {
    for (int i = 0; i < value.length(); i++) {
      try {
        JSONObject x = value.getJSONObject(i);
        output.put(
            new BusServiceKey(x.getString("ServiceNo"), x.getInt("Direction")),
            new BusService(
                new String[] {
                  x.optString("AM_Offpeak_Freq"),
                  x.optString("PM_Offpeak_Freq"),
                  x.optString("AM_Peak_Freq"),
                  x.optString("PM_Peak_Freq")
                }));
      } catch (JSONException e) {
        log.error(e.getMessage());
      }
    }
  }
}
