package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DatamallBus<T1, T2> implements Callable<HashMap<T1, T2>> {
  HashMap<T1, T2> output;

  abstract String initDatamallType();

  abstract void processData(JsonArray value);

  private JsonObject connect(String urlPath) {

    String urlString = "http://datamall2.mytransport.sg/ltaodataservice/" + urlPath;
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest req =
          HttpRequest.newBuilder(URI.create(urlString))
              .header("AccountKey", DatastoreHandler.getValue("DATAMALL_API_KEY"))
              .build();
      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      return (JsonObject) JsonParser.parseString(res.body().toString());
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return new JsonObject();
  }

  public HashMap<T1, T2> call() {
    // log.debug("Starting "+Thread.currentThread().getName());
    String datamallType = initDatamallType();
    output = new HashMap<T1, T2>();
    JsonObject json;
    JsonArray arr;
    while (true) {
      json = connect(datamallType + "?$skip=" + output.size());
      arr = json.get("value").getAsJsonArray();
      if (arr.size() == 0) break; // End condition
      processData(arr);
    }
    // log.debug("Returning "+Thread.currentThread().getName());
    return output;
  }

  String optString(JsonElement e) {
    if (e.isJsonNull()) return "";
    else return e.getAsString();
  }

  double optDouble(JsonElement e) {
    if (e.isJsonNull()) return Double.NaN;
    else return e.getAsDouble();
  }
}
