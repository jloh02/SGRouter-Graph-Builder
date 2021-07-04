package com.jonathan.sgrouter.graphbuilder.builders;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.datamall.*;
import com.jonathan.sgrouter.graphbuilder.models.Node;
import com.jonathan.sgrouter.graphbuilder.models.Vertex;
import com.jonathan.sgrouter.graphbuilder.utils.*;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@AllArgsConstructor
public class BusGraphBuilder implements Callable<ArrayList<Node>> {
  SQLiteHandler sqh;
  double busSpeed, busStopTime;

  public ArrayList<Node> call() { // Bus speed in km per minute
    /*----------------------Import data from Datamall----------------------*/
    ExecutorService executor = Executors.newFixedThreadPool(3);
    Future<HashMap<String, BusStop>> busStopFuture = executor.submit(new BusStopData());
    Future<HashMap<BusServiceKey, BusService>> busServiceFuture =
        executor.submit(new BusServiceData());
    Future<HashMap<BusRouteKey, BusRoute>> busRouteFuture = executor.submit(new BusRouteData());

    HashMap<String, BusStop> importedBusStops;
    HashMap<BusServiceKey, BusService> importedBusServices;
    HashMap<BusRouteKey, BusRoute> importedBusRoutes;
    try {
      importedBusStops = busStopFuture.get();
      importedBusServices = busServiceFuture.get();
      importedBusRoutes = busRouteFuture.get();
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread failed");
    }

    // log.trace(importedBusStops.toString().substring(0,1000));
    // log.trace(importedBusServices.toString().substring(0,1000));
    // log.trace(importedBusRoutes.toString().substring(0,1000));

    // log.trace("\n----------------\nNon-matching bus stops:");
    // for (String s : importedBusStops.keySet()) if(!s.matches("^\\d{5}"))
    // log.trace(s);

    // log.trace("----------------");
    // for (BusServiceKey s : importedBusServices.keySet())
    // if(!s.service.matches("^(\\d{1,3}[ABCEGMNRWXe]?)|(NR\\d)|(CT8)|(CT18)"))
    // log.trace(s);
    // log.trace("----------------");

    /*----------------------Generate list of sorted keys for iteration----------------------*/
    ArrayList<String> sortedBusStops = new ArrayList<>(importedBusStops.keySet());
    Collections.sort(sortedBusStops);
    ArrayList<BusServiceKey> sortedBusServices = new ArrayList<>(importedBusServices.keySet());
    Collections.sort(sortedBusServices);
    ArrayList<BusRouteKey> sortedBusRoutes = new ArrayList<>(importedBusRoutes.keySet());
    Collections.sort(sortedBusRoutes);

    // log.trace(sortedBusStops.toString().substring(0, 100));
    // log.trace(sortedBusServices.toString().substring(0, 100));
    // log.trace(sortedBusRoutes.toString().substring(0, 1000));

    /*----------------------Generate bus frequency list----------------------*/
    HashMap<String, Double> freqList = new HashMap<>();
    for (Entry<BusServiceKey, BusService> ent : importedBusServices.entrySet()) {
      BusServiceKey k = ent.getKey();
      BusService s = ent.getValue();
      double f = Utils.getFreq(s.getFreqArr());
      if (f > 0) freqList.put(k.service, f);
    }

    /*----------------------Generate bus adjacency list----------------------*/
    HashSet<String> srcList = new HashSet<>();
    ArrayList<Vertex> vtxList = new ArrayList<>();

    ZonedDateTime sgNow = GraphBuilderApplication.sgNow;
    for (int i = 0; i < sortedBusRoutes.size(); i++) {
      BusRouteKey srcRouteKey = sortedBusRoutes.get(i);
      BusRoute srcRouteData = importedBusRoutes.get(srcRouteKey);

      // Handles abnormal cases such as CTE expressway as a "node"
      if (!Utils.isBusStop(srcRouteData.src)) continue;

      srcList.add(srcRouteData.src);

      // log.trace(srcRouteData);
      // log.trace(serviceData);

      // Check if operational
      ZonedDateTime firstBus, lastBus;
      if (sgNow.getDayOfWeek() == DayOfWeek.SATURDAY) {
        if (srcRouteData.SAT_first.equals("-")) continue;
        firstBus = getFirstLastBusDT(sgNow, srcRouteData.SAT_first);
        lastBus = getFirstLastBusDT(sgNow, srcRouteData.SAT_last);
      } else if (sgNow.getDayOfWeek() == DayOfWeek.SUNDAY) {
        if (srcRouteData.SUN_first.equals("-")) continue;
        firstBus = getFirstLastBusDT(sgNow, srcRouteData.SUN_first);
        lastBus = getFirstLastBusDT(sgNow, srcRouteData.SUN_last);
      } else {
        if (srcRouteData.WD_first.equals("-")) continue;
        firstBus = getFirstLastBusDT(sgNow, srcRouteData.WD_first);
        lastBus = getFirstLastBusDT(sgNow, srcRouteData.WD_last);
      }
      if (firstBus.isAfter(lastBus))
        lastBus = lastBus.plusDays(1); // Handle for lastBus after 00:00
      if (sgNow.isBefore(firstBus) || sgNow.isAfter(lastBus)) continue;

      if (!freqList.containsKey(srcRouteKey.service))
        continue; // Service not available during non-peak but still within first/last bus range

      if (i + 1 < sortedBusRoutes.size()
          && sortedBusRoutes.get(i + 1).direction == srcRouteKey.direction
          && sortedBusRoutes.get(i + 1).service.equals(srcRouteKey.service)) {

        BusRouteKey desRouteKey = sortedBusRoutes.get(i + 1);
        BusRoute desRouteData = importedBusRoutes.get(desRouteKey);

        if (!Utils.isBusStop(desRouteData.src)) continue;
        double travelTime = (desRouteData.distance - srcRouteData.distance) / busSpeed;

        vtxList.add(
            new Vertex(srcRouteData.src, desRouteData.src, srcRouteKey.service, travelTime));
      }
    }

    /*----------------------Generate output: Bus coordinates list----------------------*/
    ArrayList<Node> nodeList = new ArrayList<>();
    for (String src : srcList) {
      BusStop bsData = importedBusStops.get(src);
      nodeList.add(new Node(src, bsData.description, bsData.lat, bsData.lon));
    }

    /*----------------------Update DB----------------------*/
    sqh.addNodes(nodeList);
    sqh.addVertices(vtxList);
    sqh.addFreq(freqList);

    log.debug("Bus graph created");

    return nodeList;
  }

  static ZonedDateTime getFirstLastBusDT(ZonedDateTime now, String fl_bus) {
    if (fl_bus.equals("2400")) fl_bus = "2359";
    return now.withHour(Integer.parseInt(fl_bus.substring(0, 2)))
        .withMinute(Integer.parseInt(fl_bus.substring(2)))
        .withSecond(0);
  }
}
