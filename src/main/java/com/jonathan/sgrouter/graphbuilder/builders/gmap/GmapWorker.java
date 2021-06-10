package com.jonathan.sgrouter.graphbuilder.builders.gmap;

import com.google.maps.model.LatLng;
import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GmapWorker {
  List<String> srcList = new ArrayList<>(),
      desList = new ArrayList<>(),
      servList = new ArrayList<>();
  GmapTiming defaultTiming;
  GmapConnection gmapConn;

  public GmapWorker() {
    gmapConn = new GmapConnection();
  }

  public double getWalkSpeed(double srcLat, double srcLon, double desLat, double desLon) {
    try {
      return gmapConn.getWalkSpeed(new LatLng(srcLat, srcLon), new LatLng(desLat, desLon));
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return GraphBuilderApplication.config.graphbuilder.getDefaultWalkingSpeed();
  }

  public GmapTiming getAvgTiming(TransitMode mode) {
    double numValid = 0.0, sumSpeed = 0.0, sumStopTime = 0.0;
    for (int i = 0; i < srcList.size(); i++) {
      GmapTiming t =
          gmapConn.getGmapSpeed(
              srcList.get(i), desList.get(i), mode, servList.get(i), defaultTiming);
      log.debug(t.toString());
      if (!t.equals(defaultTiming)) {
        numValid++;
        sumSpeed += t.speed;
        sumStopTime += t.stopTime;
      }
    }
    if (numValid == 0) return defaultTiming;
    return new GmapTiming(sumSpeed / numValid, sumStopTime / numValid);
  }

  public void setDefaultTiming(GmapTiming defaultTiming) {
    this.defaultTiming = defaultTiming;
  }

  public void add(String src, String des, String serv) {
    srcList.add(src);
    desList.add(des);
    servList.add(serv);
  }

  public void clear() {
    srcList.clear();
    desList.clear();
    servList.clear();
  }

  public void close() {
    gmapConn.close();
  }
}
