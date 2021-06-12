package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.BusGraphBuilder;
import com.jonathan.sgrouter.graphbuilder.builders.TrainGraphBuilder;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.calibration.Calibration;
import com.jonathan.sgrouter.graphbuilder.models.Node;
import com.jonathan.sgrouter.graphbuilder.models.Vertex;
import com.jonathan.sgrouter.graphbuilder.utils.CloudStorageHandler;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import com.jonathan.sgrouter.graphbuilder.utils.SQLiteHandler;
import com.jonathan.sgrouter.graphbuilder.utils.Utils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    SQLiteHandler sqh = new SQLiteHandler();

    ZonedDateTime serverNow = ZonedDateTime.now();
    GraphBuilderApplication.sgNow = serverNow.withZoneSameInstant(ZoneId.of("Asia/Singapore"));

    GmapTiming[] timings = Calibration.calibrateSpeeds();
    DatastoreHandler.setWalkSpeed(timings[3].speed);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    Future<ArrayList<Node>> busGraphFuture =
        executor.submit(new BusGraphBuilder(sqh, timings[0].speed, timings[0].stopTime));
    Future<ArrayList<Node>> trainGraphFuture =
        executor.submit(
            new TrainGraphBuilder(
                sqh,
                timings[1].speed,
                timings[1].stopTime,
                timings[2].speed,
                timings[2].stopTime,
                timings[3].speed));

    ArrayList<Node> busGraph, trainGraph;
    try {
      busGraph = busGraphFuture.get();
      trainGraph = trainGraphFuture.get();
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread failed");
    }

    // Train-Bus Vertices
    ArrayList<Vertex> busTrainVtx = new ArrayList<>();
    try {
      GeodeticCalculator gc = new GeodeticCalculator(CRS.parseWKT(Utils.getLatLonWKT()));
      for (Node bus : busGraph) {
        for (Node train : trainGraph) {
          gc.setStartingGeographicPoint(bus.getLon(), bus.getLat());
          gc.setDestinationGeographicPoint(train.getLon(), train.getLat());
          double dist = gc.getOrthodromicDistance() / 1000.0;
          if (dist <= GraphBuilderApplication.config.graphbuilder.getMaximumBusTrainDist()) {
            busTrainVtx.add(
                new Vertex(
                    bus.getSrcKey(),
                    train.getSrcKey(),
                    "Walk (Bus-Train)",
                    dist / timings[3].speed));
            busTrainVtx.add(
                new Vertex(
                    train.getSrcKey(),
                    bus.getSrcKey(),
                    "Walk (Train-Bus)",
                    dist / timings[3].speed));
          }
        }
      }
      sqh.addVertices(busTrainVtx);
    } catch (FactoryException e) {
      log.error(e.getMessage());
    }
    // Commit transactions to DB
    sqh.commit();

    CloudStorageHandler.uploadDB();

    return true;
  }
}
