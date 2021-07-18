package com.jonathan.sgrouter.graphbuilder.calibration;

import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import java.time.Instant;
import java.util.concurrent.Callable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CalibMrt implements Callable<GmapTiming> {
  final Instant dt;

  public GmapTiming call() {
    /*------------------------------------------ MRT SPEED ------------------------------------------*/
    // DT Line: Bukit Panjang → Upper Changi
    // NS Line: Bukit Batok → Marina South Pier
    // EW Line: Bedok → Tuas Link
    // CC Line: Lorong Chuan → HarbourFront
    // NE Line: Punggol → HarbourFront
    /*-----------------------------------------------------------------------------------------------*/
    GmapWorker gw = new GmapWorker();
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.train.getMrt()));
    gw.add("ChIJARt1T4kR2jERTyE-4kybHC4", "ChIJtcq9et882jERXFLqMR93eiw", "Expo");
    gw.add("ChIJy37FHD8Q2jERQrs731bZT5Y", "ChIJq0GQRB8Z2jERfz0GTv95OQg", "Marina South Pier");
    gw.add("ChIJl_Hgw9oj2jERYhgt4z-6OwI", "ChIJsadYyEwP2jERL7SpvzQCd5Q", "Tuas Link");
    gw.add("ChIJK2ewIQkX2jERNNpWkYHNPDk", "ChIJwZI-B-Ib2jERG0UqkScDu7s", "Harbour Front");
    gw.add("ChIJB9fWN-MV2jER0btc565fpUA", "ChIJwZI-B-Ib2jERG0UqkScDu7s", "Harbour Front");
    return gw.getAvgTiming(TransitMode.SUBWAY, dt);
  }
}
