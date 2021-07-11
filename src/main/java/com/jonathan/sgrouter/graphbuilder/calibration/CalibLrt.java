package com.jonathan.sgrouter.graphbuilder.calibration;

import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import java.util.concurrent.Callable;

public class CalibLrt implements Callable<GmapTiming> {
  public GmapTiming call() {
    /*------------------------------------------ LRT SPEED ------------------------------------------*/
    // STC: Thanggam → Sengkang
    // PTC: Riviera → Punggol
    // BP LRT: Choa Chu Kang → Bukit Panjang
    /*-----------------------------------------------------------------------------------------------*/
    GmapWorker gw = new GmapWorker();
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.train.getLrt()));
    gw.add("ChIJ3TdadkoX2jERFPpqYTr14uA", "ChIJW8xBftIX2jER0iArkCKzqZ8", "Sengkang");
    gw.add("ChIJrSYX8Pg92jERCBdEFSd-FGU", "ChIJB9fWN-MV2jER0btc565fpUA", "Punggol");
    gw.add("ChIJf-qKBzYR2jERPCaE21v1ssk", "ChIJD58--UkR2jERMi5sKsqSqko", "Bukit Panjang");
    return gw.getAvgTiming(TransitMode.SUBWAY);
  }
}
