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
  final static String bucketName = "sg-router.appspot.com";
  final static String objectName = "graph.db";

  public static void uploadDB() {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    BlobId blobId = BlobId.of(bucketName, objectName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    try {
      storage.create(blobInfo, Files.readAllBytes(Paths.get(dbPath)));
    } catch (IOException e) {
      System.err.println(e);
    }

    System.out.println("File " + dbPath + " uploaded to bucket " + bucketName);
  }
}
