package com.jonathan.sgrouter.graphbuilder.calibration;

import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import java.util.HashSet;
import java.util.Map.Entry;

public class Calibration {
  public static CalibMap calibrateSpeeds() {

    CalibMap op = new CalibMap();

    GmapWorker gw = new GmapWorker();
    gw.setTransitMode(TransitMode.SUBWAY);

    /*------------------------------------------ MRT SPEED ------------------------------------------*/
    // DT Line: Bukit Panjang → Upper Changi
    // NS Line: Bukit Batok → Marina South Pier
    // EW Line: Bedok → Tuas Link
    // CC Line: Lorong Chuan → HarbourFront
    // NE Line: Punggol → HarbourFront
    /*-----------------------------------------------------------------------------------------------*/
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.train.getMrt()));
    op.put(
        "DT", gw.getTiming("ChIJARt1T4kR2jERTyE-4kybHC4", "ChIJtcq9et882jERXFLqMR93eiw", "Expo"));
    op.put(
        "NS",
        gw.getTiming(
            "ChIJy37FHD8Q2jERQrs731bZT5Y", "ChIJq0GQRB8Z2jERfz0GTv95OQg", "Marina South Pier"));
    op.put(
        "EW",
        gw.getTiming("ChIJl_Hgw9oj2jERYhgt4z-6OwI", "ChIJsadYyEwP2jERL7SpvzQCd5Q", "Tuas Link"));
    op.put(
        "CC",
        gw.getTiming(
            "ChIJK2ewIQkX2jERNNpWkYHNPDk", "ChIJwZI-B-Ib2jERG0UqkScDu7s", "Harbour Front"));
    op.put(
        "NE",
        gw.getTiming(
            "ChIJB9fWN-MV2jER0btc565fpUA", "ChIJwZI-B-Ib2jERG0UqkScDu7s", "Harbour Front"));

    // Speed factor to account for differences between gmap and graph distance
    // In trains, straight line vs actual route distance
    HashSet<String> processed = new HashSet<>();
    for (Entry<String, GmapTiming> ent : op.entrySet()) {
      processed.add(ent.getKey());
      ent.getValue().speed *=
          GraphBuilderApplication.config.graphbuilder.train.mrt.getSpeedFactor();
    }

    /*------------------------------------------ LRT SPEED ------------------------------------------*/
    // STC: Thanggam → Sengkang
    // PTC: Riviera → Punggol
    // BP LRT: Choa Chu Kang → Bukit Panjang
    /*-----------------------------------------------------------------------------------------------*/
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.train.getLrt()));
    op.put(
        "ST",
        gw.getTiming("ChIJ3TdadkoX2jERFPpqYTr14uA", "ChIJW8xBftIX2jER0iArkCKzqZ8", "Sengkang"));
    op.put(
        "PT",
        gw.getTiming("ChIJrSYX8Pg92jERCBdEFSd-FGU", "ChIJB9fWN-MV2jER0btc565fpUA", "Punggol"));
    op.put(
        "BP",
        gw.getTiming(
            "ChIJf-qKBzYR2jERPCaE21v1ssk", "ChIJD58--UkR2jERMi5sKsqSqko", "Bukit Panjang"));

    for (Entry<String, GmapTiming> ent : op.entrySet()) {
      if (processed.contains(ent.getKey())) continue;
      ent.getValue().speed *=
          GraphBuilderApplication.config.graphbuilder.train.lrt.getSpeedFactor();
    }

    return op;
  }
}
