package com.jonathan.sgrouter.graphbuilder.calibration;

import java.util.concurrent.Callable;

import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;

public class CalibWalk implements Callable<GmapTiming>{
  public GmapTiming call(){
    GmapWorker gw = new GmapWorker();
    return new GmapTiming(gw.getWalkSpeed(1.330302, 103.645823, 1.372498, 103.982303), 0);
  }
}
