package com.jonathan.sgrouter.graphbuilder.builders.gmap;

import com.google.maps.model.LatLng;
import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GmapWorker {
  GmapTiming defaultTiming;
  GmapConnection gmapConn;
  TransitMode mode;

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

  public GmapTiming getTiming(String src, String des, String serv) {
    return gmapConn.getGmapSpeed(src, des, mode, serv, defaultTiming);
  }

  public void setDefaultTiming(GmapTiming defaultTiming) {
    this.defaultTiming = defaultTiming;
  }

  public void setTransitMode(TransitMode mode) {
    this.mode = mode;
  }
}
