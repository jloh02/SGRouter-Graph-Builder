package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.builders.BuilderRunner;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapConnection;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class GraphBuilderController {
  @GetMapping("/ping")
  public String ping() {
    return "pong";
  }

  // SHA1 hash of generateGraph
  @GetMapping("74004ad79c7fcb8f4a22450b6f372c240bd09f49")
  public boolean generateGraph() {
    GmapConnection.open();

    ZonedDateTime serverNow = ZonedDateTime.now().withSecond(0);

    GmapWorker gw = new GmapWorker();
    GmapTiming walkGT =
        new GmapTiming(gw.getWalkSpeed(1.330302, 103.645823, 1.372498, 103.982303), 0);

    int prog = DatastoreHandler.getProgress();
    int hStart = prog / 60;
    int mStart = prog % 60;
    log.debug("-----Starting graph build from {}:{}-----", hStart, mStart);
    for (int hour = hStart; hour < 24; hour++) {
      for (int minute = mStart; minute < 60; minute += 5) {
        BuilderRunner.run(serverNow, hour, minute, walkGT);
        DatastoreHandler.setProgress(hour * 60 + minute + 5);
      }
    }
    GmapConnection.close();
    return true;
  }
}
