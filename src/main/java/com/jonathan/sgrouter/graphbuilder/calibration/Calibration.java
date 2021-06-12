package com.jonathan.sgrouter.graphbuilder.calibration;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapConnection;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class Calibration {
  public static GmapTiming[] calibrateSpeeds() {

    GmapTiming[] timings = new GmapTiming[4]; // Bus,Mrt,Lrt,Walk

    ExecutorService executor = Executors.newFixedThreadPool(3);
    Future<GmapTiming> busTiming = executor.submit(new CalibBus());
    Future<GmapTiming> mrtTiming = executor.submit(new CalibMrt());
    Future<GmapTiming> lrtTiming = executor.submit(new CalibLrt());
    Future<GmapTiming> walkTiming = executor.submit(new CalibWalk());
    try {
      timings[0] = busTiming.get();
      timings[1] = mrtTiming.get();
      timings[2] = lrtTiming.get();
      timings[3] = walkTiming.get();
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread failed");
    }

    // Speed factor to account for differences between gmap and graph distance
    // In trains, straight line vs actual route distance
    timings[0].speed *= GraphBuilderApplication.config.graphbuilder.bus.getSpeedFactor();
    timings[1].speed *= GraphBuilderApplication.config.graphbuilder.train.mrt.getSpeedFactor();
    timings[2].speed *= GraphBuilderApplication.config.graphbuilder.train.lrt.getSpeedFactor();

    GmapConnection.close();
    return timings;
  }
}
