package com.jonathan.sgrouter.graphbuilder.utils;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CloudStorageHandler {
  final static String dbPath = GraphBuilderApplication.config.isAppengineDeployment() ? "/tmp/graph.db" : "graph.db";
  final static String bucket = "sg-router.appspot.com";
  final static String filename = "graph.db";

  public static void uploadDB() {
    Storage store = StorageOptions.getDefaultInstance().getService();
    BlobInfo bInf = BlobInfo.newBuilder(BlobId.of(bucket, filename)).build();
    try {
      store.create(bInf, Files.readAllBytes(Paths.get(dbPath)));
    } catch (IOException e) {
      System.err.println(e);
    }

    System.out.println("Uploaded graph.db");
  }
}
