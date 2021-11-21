package com.jonathan.sgrouter.graphbuilder.utils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/** Static class to handle retrieval of key-value pairs from Google Datastore */
public class DatastoreHandler {
  private static final HashMap<String, String> values = new HashMap<>();
  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private static boolean walkSpeedSet = false;

  /**
   * Retrieves values from Google Cloud Datastore with the namespace
   * @param name    the key
   * @return        a @link{java.lang.String} value 
   */
  public static String getValue(String name) {
    if (values.containsKey(name)) return values.get(name);
    return getKey(name);
  }

  public static void setWalkSpeed(double speed) {
    if (walkSpeedSet) return;
    Key taskKey =
        datastore.newKeyFactory().setNamespace("sgrouter").setKind("constants").newKey("walkSpeed");
    Entity task = Entity.newBuilder(taskKey).set("value", speed).build();
    datastore.put(task);
    walkSpeedSet = true;
  }

  private static String getKey(String name) {
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

  public static void setLastModTiming() {
    Key taskKey =
        datastore
            .newKeyFactory()
            .setNamespace("sgrouter")
            .setKind("constants")
            .newKey("graph-last-modified");
    Entity task = Entity.newBuilder(taskKey).set("value", Timestamp.now()).build();
    datastore.put(task);
  }
}
