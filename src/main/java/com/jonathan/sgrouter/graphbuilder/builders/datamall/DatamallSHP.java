package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.geotools.ShpNode;
import com.jonathan.sgrouter.graphbuilder.builders.geotools.ShpParser;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class DatamallSHP {
  static String fileDir = "";
  static String filename = "";

  // Returns path to SHP-related files with filenames without extension
  public static ArrayList<ShpNode> getSHP(String layerID) {
    fileDir = GraphBuilderApplication.config.appengineDeployment ? "/tmp/" + layerID : layerID;
    filename = fileDir + ".zip";

    deleteTmpFiles();
    downloadZIP(getLink(layerID));

    String dir = extractZip();
    String[] shpFiles = new File(dir).list();
    String baseName = shpFiles[0].split("\\.")[0];

    ArrayList<ShpNode> out =
        ShpParser.parse(String.format("%s/%s", dir, baseName), layerID.equals("TrainStationExit"));

    deleteTmpFiles();
    return out;
  }

  private static String getLink(String layerID) { // TrainStation or TrainStationExit
    String urlString =
        "http://datamall2.mytransport.sg/ltaodataservice/GeospatialWholeIsland?ID=" + layerID;
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest req =
          HttpRequest.newBuilder(URI.create(urlString))
              .header("AccountKey", DatastoreHandler.getValue("DATAMALL_API_KEY"))
              .build();
      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      return new JSONObject(res.body().toString())
          .getJSONArray("value")
          .getJSONObject(0)
          .getString("Link");
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return "";
  }

  private static void downloadZIP(String link) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest req = HttpRequest.newBuilder(URI.create(link)).build();
      client.send(req, HttpResponse.BodyHandlers.ofFile(new File(filename).toPath()));
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  // Returns path to zip
  private static String extractZip() {
    new File(fileDir).delete();
    new File(fileDir).mkdir();

    String parName = "";
    try {
      ZipFile zip = new ZipFile(filename);
      Enumeration<? extends ZipEntry> zipEntries = zip.entries();
      while (zipEntries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) zipEntries.nextElement();
        String entryName = entry.getName();

        File desFile = new File(fileDir, entryName);

        // Handle parent directory
        File par = desFile.getParentFile();
        par.mkdirs();
        parName = par.getPath();

        if (!entry.isDirectory()) {
          BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
          int currentByte;
          byte data[] = new byte[4096];
          FileOutputStream fos = new FileOutputStream(desFile);
          BufferedOutputStream dest = new BufferedOutputStream(fos, 4096);
          while ((currentByte = is.read(data, 0, 4096)) != -1) dest.write(data, 0, currentByte);
          dest.flush();
          dest.close();
          is.close();
        }
      }
      zip.close();
      return parName;
    } catch (IOException e) {
      throw new NullPointerException(e.toString());
    }
  }

  private static void deleteTmpFiles() {
    if (new File(fileDir).exists()) {
      try {
        Stream<Path> walk = Files.walk(Paths.get(fileDir));
        walk.sorted(Comparator.reverseOrder()).forEach(DatamallSHP::deleteLambda);
        walk.close();
      } catch (IOException e) {
        log.error(e.getMessage());
      }
    }
    new File(filename).delete();
  }

  private static void deleteLambda(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }
}
