package com.jonathan.sgrouter.graphbuilder.controllers;

import com.google.maps.model.TransitMode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.BusGraphBuilder;
import com.jonathan.sgrouter.graphbuilder.builders.MrtGraphBuilder;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapTiming;
import com.jonathan.sgrouter.graphbuilder.builders.gmap.GmapWorker;
import com.jonathan.sgrouter.graphbuilder.models.Node;
import com.jonathan.sgrouter.graphbuilder.models.Vertex;
import com.jonathan.sgrouter.graphbuilder.utils.CloudStorageHandler;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import com.jonathan.sgrouter.graphbuilder.utils.SQLiteHandler;
import com.jonathan.sgrouter.graphbuilder.utils.Utils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;
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
    SQLiteHandler sqh = new SQLiteHandler();

    ZonedDateTime serverNow = ZonedDateTime.now();
    GraphBuilderApplication.sgNow = serverNow.withZoneSameInstant(ZoneId.of("Asia/Singapore"));

    GmapTiming[] timings = calibrateSpeeds();
    DatastoreHandler.setWalkSpeed(timings[3].speed);

    //TODO Multithread bulding each graph
    List<Node> busGraph = BusGraphBuilder.build(sqh, timings[0].speed, timings[0].stopTime);
    List<Node> trainGraph =
        MrtGraphBuilder.build(
            sqh,
            timings[1].speed,
            timings[1].stopTime,
            timings[2].speed,
            timings[2].stopTime,
            timings[3].speed);

    // Train-Bus Vertices
    List<Vertex> busTrainVtx = new ArrayList<>();
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

  GmapTiming[] calibrateSpeeds() {
    GmapWorker gw = new GmapWorker();
    GmapTiming[] timings = new GmapTiming[4]; // Bus,Mrt,Lrt,Walk

    /*------------------------------------------ BUS SPEED ------------------------------------------*/
    // Bus 54: Bishan Int → Kampong Bahru Ter
    // Bus 30: Bedok Int → Boon Lay Int
    // Bus 854: Yishun Int → Bedok Int
    // Bus 196: Bedok Int → Clementi Int
    // Bus 67: Choa Chu Kang Int → Tampines Int
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.bus));
    gw.add("ChIJlZhfpRYX2jERttwvFZslalU", "ChIJvVHZRG8Z2jERoxDuLFtRZgc", "54");
    gw.add("ChIJa1PPHbMi2jERKCuIMcgqSFs", "ChIJlwb0K5MP2jERheBVJtHCP1w", "30");
    gw.add("ChIJYdfkYW8U2jERzA4NiwUkuOw", "ChIJa1PPHbMi2jERKCuIMcgqSFs", "854");
    gw.add("ChIJa1PPHbMi2jERKCuIMcgqSFs", "ChIJT0PyK44a2jER1jVN-Eu18cI", "196");
    gw.add("ChIJxe0CeekR2jERTvo3i83VqUY", "ChIJFczkAA492jERUI8_NFpELcg", "67");
    timings[0] = gw.getAvgTiming(TransitMode.BUS);
    log.debug("-----------");
    gw.clear();
    /*-----------------------------------------------------------------------------------------------*/

    /*------------------------------------------ MRT SPEED ------------------------------------------*/
    // DT Line: Bukit Panjang → Upper Changi
    // NS Line: Bukit Batok → Marina South Pier
    // EW Line: Bedok → Tuas Link
    // CC Line: Lorong Chuan → HarbourFront
    // NE Line: Punggol → HarbourFront
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.train.getMrt()));
    gw.add("ChIJARt1T4kR2jERTyE-4kybHC4", "ChIJtcq9et882jERXFLqMR93eiw", "Expo");
    gw.add("ChIJy37FHD8Q2jERQrs731bZT5Y", "ChIJq0GQRB8Z2jERfz0GTv95OQg", "Marina South Pier");
    gw.add("ChIJl_Hgw9oj2jERYhgt4z-6OwI", "ChIJsadYyEwP2jERL7SpvzQCd5Q", "Tuas Link");
    gw.add("ChIJK2ewIQkX2jERNNpWkYHNPDk", "ChIJwZI-B-Ib2jERG0UqkScDu7s", "Harbour Front");
    gw.add("ChIJB9fWN-MV2jER0btc565fpUA", "ChIJwZI-B-Ib2jERG0UqkScDu7s", "Harbour Front");
    timings[1] = gw.getAvgTiming(TransitMode.SUBWAY);
    log.debug("-----------");
    gw.clear();
    /*-----------------------------------------------------------------------------------------------*/

    /*------------------------------------------ LRT SPEED ------------------------------------------*/
    // STC: Thanggam → Sengkang
    // PTC: Riviera → Punggol
    // BP LRT: Choa Chu Kang → Bukit Panjang
    gw.setDefaultTiming(new GmapTiming(GraphBuilderApplication.config.graphbuilder.train.getLrt()));
    gw.add("ChIJ3TdadkoX2jERFPpqYTr14uA", "ChIJW8xBftIX2jER0iArkCKzqZ8", "Sengkang");
    gw.add("ChIJrSYX8Pg92jERCBdEFSd-FGU", "ChIJB9fWN-MV2jER0btc565fpUA", "Punggol");
    gw.add("ChIJf-qKBzYR2jERPCaE21v1ssk", "ChIJD58--UkR2jERMi5sKsqSqko", "Bukit Panjang");
    timings[2] = gw.getAvgTiming(TransitMode.SUBWAY);
    log.debug("-----------");
    gw.clear();
    /*-----------------------------------------------------------------------------------------------*/

    timings[3] = new GmapTiming(gw.getWalkSpeed(1.330302, 103.645823, 1.372498, 103.982303), 0);
    gw.close();

    // Speed factor to account for differences between gmap and graph distance
    // In trains, straight line vs actual route distance
    timings[0].speed *= GraphBuilderApplication.config.graphbuilder.bus.getSpeedFactor();
    timings[1].speed *= GraphBuilderApplication.config.graphbuilder.train.mrt.getSpeedFactor();
    timings[2].speed *= GraphBuilderApplication.config.graphbuilder.train.lrt.getSpeedFactor();

    return timings;
  }
}
