package com.jonathan.sgrouter.graphbuilder.builders;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
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
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class BuilderRunner {
  public static void run(ZonedDateTime serverNow, double walkSpeed) {
    ZonedDateTime sgTime = serverNow.withZoneSameInstant(ZoneId.of("Asia/Singapore"));

    int hour = sgTime.getHour();
    if (hour >= 2 && hour <= 4) return;

    String dbName = "graph.db";
    SQLiteHandler sqh = new SQLiteHandler(dbName);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    Future<ArrayList<Node>> busGraphFuture = executor.submit(new BusGraphBuilder(sqh, sgTime));
    Future<ArrayList<Node>> trainGraphFuture =
        executor.submit(new TrainGraphBuilder(sqh, walkSpeed, sgTime));

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
                    bus.getSrcKey(), train.getSrcKey(), "Walk (Bus-Train)", dist / walkSpeed));
            busTrainVtx.add(
                new Vertex(
                    train.getSrcKey(), bus.getSrcKey(), "Walk (Train-Bus)", dist / walkSpeed));
          }
        }
      }
      sqh.addVertices(busTrainVtx);
    } catch (FactoryException e) {
      log.error(e.getMessage());
    }
    // Commit transactions to DB
    sqh.commit();
    CloudStorageHandler.uploadDB(dbName);
    DatastoreHandler.setLastModTiming();
  }
}
