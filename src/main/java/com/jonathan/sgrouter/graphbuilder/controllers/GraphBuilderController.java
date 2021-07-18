package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.builders.BuilderRunner;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapConnection;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import java.time.ZonedDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphBuilderController {
  @GetMapping("/ping")
  public String ping() {
    return "pong";
  }

  // SHA1 hash of generateGraph
  @GetMapping("74004ad79c7fcb8f4a22450b6f372c240bd09f49")
  // Split request into 6 separate blocks of 4h to prevent execution time limit
  public boolean generateGraph(@RequestParam int hLow, @RequestParam int hHigh) {
    GmapConnection.open();

    ZonedDateTime serverNow = ZonedDateTime.now().withSecond(0);

    GmapWorker gw = new GmapWorker();
    GmapTiming walkGT =
        new GmapTiming(gw.getWalkSpeed(1.330302, 103.645823, 1.372498, 103.982303), 0);

    for (int hour = hLow; hour < hHigh; hour++) {
      for (int minute = 0; minute < 60; minute += 5) {
        BuilderRunner.run(serverNow, hLow, hHigh, hour, minute, walkGT);
      }
    }
    GmapConnection.close();
    return true;
  }
}
