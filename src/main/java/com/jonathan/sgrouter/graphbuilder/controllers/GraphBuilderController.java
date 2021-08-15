package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.builders.BuilderRunner;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapConnection;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import java.time.ZonedDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    double walkT = gw.getWalkSpeed(1.330302, 103.645823, 1.372498, 103.982303);
    DatastoreHandler.setWalkSpeed(walkT);

    BuilderRunner.run(serverNow, walkT);

    return true;
  }
}
