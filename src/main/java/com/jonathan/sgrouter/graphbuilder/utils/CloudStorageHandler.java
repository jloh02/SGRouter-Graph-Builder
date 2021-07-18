package com.jonathan.sgrouter.graphbuilder.utils;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloudStorageHandler {
  static final String bucket = "sg-router.appspot.com";

  public static void uploadDB(String dbPath) {
    String dbName = dbPath;
    dbPath = "dbs/" + dbPath;
    if (GraphBuilderApplication.appengineDeployment) dbPath = "/tmp/" + dbPath;

    Storage store = StorageOptions.getDefaultInstance().getService();
    BlobInfo bInf = BlobInfo.newBuilder(BlobId.of(bucket, dbName)).build();
    try {
      store.create(bInf, Files.readAllBytes(Paths.get(dbPath)));
    } catch (IOException e) {
      log.error(e.getMessage());
    }

    log.debug("Uploaded " + dbName);
  }
}
