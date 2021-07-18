package com.jonathan.sgrouter.graphbuilder.calibration;

import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import java.time.Instant;
import java.util.concurrent.Callable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CalibBus implements Callable<GmapTiming> {
  final Instant dt;

  public GmapTiming call() {
    /*------------------------------------------ BUS SPEED ------------------------------------------*/
    // Bus 54: Bishan Int → Kampong Bahru Ter
    // Bus 30: Bedok Int → Boon Lay Int
    // Bus 854: Yishun Int → Bedok Int
    // Bus 196: Bedok Int → Clementi Int
    // Bus 67: Choa Chu Kang Int → Tampines Int
    /*-----------------------------------------------------------------------------------------------*/
    GmapWorker gw = new GmapWorker();
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.bus));
    gw.add("ChIJlZhfpRYX2jERttwvFZslalU", "ChIJvVHZRG8Z2jERoxDuLFtRZgc", "54");
    gw.add("ChIJa1PPHbMi2jERKCuIMcgqSFs", "ChIJlwb0K5MP2jERheBVJtHCP1w", "30");
    gw.add("ChIJYdfkYW8U2jERzA4NiwUkuOw", "ChIJa1PPHbMi2jERKCuIMcgqSFs", "854");
    gw.add("ChIJa1PPHbMi2jERKCuIMcgqSFs", "ChIJT0PyK44a2jER1jVN-Eu18cI", "196");
    gw.add("ChIJxe0CeekR2jERTvo3i83VqUY", "ChIJFczkAA492jERUI8_NFpELcg", "67");
    return gw.getAvgTiming(TransitMode.BUS, dt);
  }
}
