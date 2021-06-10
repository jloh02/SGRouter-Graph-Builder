package com.jonathan.sgrouter.graphbuilder.utils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatastoreHandler {
  static final Map<String, String> values = new HashMap<>();
  static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  public static String getValue(String name) {
    if (values.containsKey(name)) return values.get(name);
    return getKey(name);
  }

  public static void setWalkSpeed(double speed) {
    Key taskKey =
        datastore.newKeyFactory().setNamespace("sgrouter").setKind("constants").newKey("walkSpeed");
    Entity task = Entity.newBuilder(taskKey).set("value", speed).build();
    datastore.put(task);
  }

  static String getKey(String name) {
    try {
      Key taskKey = datastore.newKeyFactory().setNamespace("sgrouter").setKind("keys").newKey(name);
      Entity retrieved = datastore.get(taskKey);
      values.put(name, retrieved.getString("value"));
      return values.get(name);
    } catch (Exception e) {
      log.error(e.getMessage());
      System.exit(1);
    }
    return "";
  }
}
