package com.jonathan.sgrouter.graphbuilder.calibration;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalibMap extends HashMap<String, GmapTiming> {
  @Override
  public GmapTiming get(Object key) {
    String k = ((String) key).substring(0, 2);
    GmapTiming ret = getSimple(k);
    if (ret == null) {
      log.warn("Calibration key not found: {}", k);
      return new GmapTiming(
          GraphBuilderApplication.config.graphbuilder.train.mrt.getDefaultSpeed(),
          GraphBuilderApplication.config.graphbuilder.train.mrt.getDefaultStopTime());
    }
    return ret;
  }

  GmapTiming getSimple(String k) {
    if (k.equals("PW") || k.equals("PE")) return super.get("PT");
    if (k.equals("SW") || k.equals("SE")) return super.get("ST");
    if (k.equals("CG")) return super.get("EW");
    if (k.equals("CE")) return super.get("CC");

    return super.get(k);
  }
}
