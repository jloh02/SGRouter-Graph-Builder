package com.jonathan.sgrouter.graphbuilder.builders;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.datamall.DatamallSHP;
import com.jonathan.sgrouter.graphbuilder.builders.geotools.ShpNode;
import com.jonathan.sgrouter.graphbuilder.models.BranchInfo;
import com.jonathan.sgrouter.graphbuilder.models.Node;
import com.jonathan.sgrouter.graphbuilder.models.Vertex;
import com.jonathan.sgrouter.graphbuilder.models.config.BranchConfig;
import com.jonathan.sgrouter.graphbuilder.models.config.TrainServiceName;
import com.jonathan.sgrouter.graphbuilder.utils.SQLiteHandler;
import com.jonathan.sgrouter.graphbuilder.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;

@Slf4j
@AllArgsConstructor
public class TrainGraphBuilder implements Callable<ArrayList<Node>> {
  public static GeodeticCalculator geoCalc;
  public static HashMap<String, TrainServiceName> serviceMap =
      new HashMap<>(GraphBuilderApplication.config.graphbuilder.train.getServices());

  SQLiteHandler sqh;
  double mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime, walkSpeed;

  public ArrayList<Node> call() { // MRT speed in km per minute
    ArrayList<ShpNode> stations = DatamallSHP.getSHP("TrainStation");
    ArrayList<ShpNode> exits = DatamallSHP.getSHP("TrainStationExit");

    stations.removeIf(x -> !Utils.isInService(x.getId()));
    exits.removeIf(x -> !Utils.isInService(x.getId()));

    Collections.sort(stations);
    Collections.sort(exits);

    /*----------------------Generate train network adjacency list----------------------*/
    HashSet<String> srcList = new HashSet<>();
    ArrayList<Vertex> vtxList = new ArrayList<>();

    ArrayList<ShpNode> stationsBaseName = new ArrayList<>();
    HashMap<String, Integer> idxs = new HashMap<>();

    HashMap<String, BranchInfo> branches = new HashMap<>();

    try {
      geoCalc = new GeodeticCalculator(CRS.parseWKT(Utils.getLatLonWKT()));

      setupData(stations, idxs, stationsBaseName, srcList);
      setupAndGenerateBranches(
          branches, stations, idxs, vtxList, mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime);
      generateLines(
          stations, idxs, vtxList, branches, mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime);
      generateInterchanges(stations, stationsBaseName, vtxList, walkSpeed);
      generateLoops(stations, idxs, vtxList, mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime);
    } catch (FactoryException e) {
      log.error(e.getMessage());
    }
    /*----------------------Generate output: Station coordinates list----------------------*/
    ArrayList<Node> nodeList = new ArrayList<>();
    for (String src : srcList) {
      ShpNode stationData = stations.get(idxs.get(src));
      nodeList.add(
          new Node(src, stationData.getName(), stationData.getLat(), stationData.getLon()));
    }

    /*----------------------Handle Exit vertices while adding to Station Exit coordinates list----------------------*/
    HashSet<String> usedExitIds = new HashSet<>();
    HashSet<String> exitStationIds = new HashSet<>();
    ArrayList<Node> exitNodeList = new ArrayList<>();

    generateExits(
        stations,
        idxs,
        vtxList,
        exits,
        usedExitIds,
        exitStationIds,
        exitNodeList,
        nodeList,
        walkSpeed);

    /*----------------------Update DB----------------------*/
    double freq = Utils.getFreq(GraphBuilderApplication.config.graphbuilder.train.getFreq());
    freq *= 0.5;
    HashMap<String, Double> freqMap = new HashMap<>();
    freqMap.put("train", freq);
    sqh.addFreq(freqMap);

    sqh.addNodes(nodeList);
    sqh.addVertices(vtxList);

    log.debug("Train graph created");

    return exitNodeList;
  }

  void setupData(
      ArrayList<ShpNode> stations,
      HashMap<String, Integer> idxs,
      ArrayList<ShpNode> stationsBaseName,
      HashSet<String> srcList) {
    for (int i = 0; i < stations.size(); i++) {
      idxs.put(stations.get(i).getId(), i);
      srcList.add(stations.get(i).getId());
      stationsBaseName.add(BuilderUtils.getBaseName(stations.get(i)));
    }
  }

  // Branch setup (Refer to branch-logic.txt)
  void setupAndGenerateBranches(
      HashMap<String, BranchInfo> branches,
      ArrayList<ShpNode> stations,
      HashMap<String, Integer> idxs,
      ArrayList<Vertex> vtxList,
      double mrtSpeed,
      double mrtStopTime,
      double lrtSpeed,
      double lrtStopTime) {
    for (BranchConfig bc : GraphBuilderApplication.config.graphbuilder.train.getBranches()) {
      if (!idxs.containsKey(bc.getSrc()) || !idxs.containsKey(bc.getDes())) continue;
      double branchDist =
          Utils.isLRT(bc.getSrc())
              ? BuilderUtils.getDistance(
                          stations.get(idxs.get(bc.getBranchNode())),
                          stations.get(idxs.get(bc.getSrc())))
                      / lrtSpeed
                  + lrtStopTime
              : BuilderUtils.getDistance(
                          stations.get(idxs.get(bc.getBranchNode())),
                          stations.get(idxs.get(bc.getSrc())))
                      / mrtSpeed
                  + mrtStopTime;
      branchDist += bc.getTransferTime();
      vtxList.add(
          new Vertex(
              bc.getBranchNode(), bc.getSrc(), bc.getBranchService().descending, branchDist));
      vtxList.add(
          new Vertex(bc.getSrc(), bc.getBranchNode(), bc.getBranchService().ascending, branchDist));
      for (int i = idxs.get(bc.getSrc()) + 1; i <= idxs.get(bc.getDes()); i++) {
        double dist =
            Utils.isLRT(bc.getSrc())
                ? BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / lrtSpeed
                    + lrtStopTime
                : BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / mrtSpeed
                    + mrtStopTime;

        vtxList.add(
            new Vertex(
                stations.get(i).getId(),
                stations.get(i - 1).getId(),
                bc.getBranchService().descending,
                dist));
        vtxList.add(
            new Vertex(
                stations.get(i - 1).getId(),
                stations.get(i).getId(),
                bc.getBranchService().ascending,
                dist));
      }
      branches.put(
          bc.getBranchNode(),
          new BranchInfo(bc.isJoin(), bc.getPostBranchService(), bc.getBranchService()));
      if (!bc.isJoin())
        throw new UnsupportedOperationException("Split branches not implemented yet");
    }
  }

  // Linear Train Vertex Creation: Generates vertices on graph which are "straight lines"
  void generateLines(
      ArrayList<ShpNode> stations,
      HashMap<String, Integer> idxs,
      ArrayList<Vertex> vtxList,
      HashMap<String, BranchInfo> branches,
      double mrtSpeed,
      double mrtStopTime,
      double lrtSpeed,
      double lrtStopTime) {

    int startIdx = 0;

    BranchInfo currBranch = null;
    while (stations
            .get(startIdx)
            .getId()
            .matches(GraphBuilderApplication.config.graphbuilder.train.getExcludeLine())
        && startIdx < stations.size()) startIdx++;
    for (int i = startIdx + 1; i < stations.size(); i++) {
      // Exclude loops from linear vertex creation
      if (stations
          .get(i)
          .getId()
          .matches(GraphBuilderApplication.config.graphbuilder.train.getExcludeLine())) {
        while (stations
                .get(i + 1)
                .getId()
                .matches(GraphBuilderApplication.config.graphbuilder.train.getExcludeLine())
            && i + 1 < stations.size()) i++;
        startIdx = i + 1;
        currBranch = null;
        i++;
        continue;
      }

      if (branches.containsKey(stations.get(i).getId())) {
        currBranch = branches.get(stations.get(i).getId());
      }

      // Add vertex if same lines, else reset line
      if (stations
          .get(i)
          .getId()
          .substring(0, 2)
          .equals(stations.get(startIdx).getId().substring(0, 2))) {

        double edgeDist = BuilderUtils.getDistance(stations.get(i - 1), stations.get(i));
        if (Utils.isLRT(stations.get(i).getId())) edgeDist = edgeDist / lrtSpeed + lrtStopTime;
        else edgeDist = edgeDist / mrtSpeed + mrtStopTime;

        if (GraphBuilderApplication.config
            .graphbuilder
            .train
            .getInvalidStations()
            .contains(stations.get(i).getId())) continue;

        TrainServiceName serv =
            currBranch == null
                ? BuilderUtils.getService(stations.get(i), stations.get(i - 1))
                : currBranch.getPostBranchService();
        vtxList.add(
            new Vertex(
                stations.get(i).getId(), stations.get(i - 1).getId(), serv.descending, edgeDist));
        vtxList.add(
            new Vertex(
                stations.get(i - 1).getId(), stations.get(i).getId(), serv.ascending, edgeDist));
      } else {
        startIdx = i;
        currBranch = null;
      }
    }
  }

  // Cross Train Lines Interchanges
  void generateInterchanges(
      ArrayList<ShpNode> stations,
      ArrayList<ShpNode> stationsBaseName,
      ArrayList<Vertex> vtxList,
      double walkSpeed) {
    for (int i = 1; i < stationsBaseName.size(); i++) {
      for (int j = 0; j < i; j++) {
        if (stationsBaseName.get(i).getName().equals(stationsBaseName.get(j).getName())
            && !stationsBaseName.get(i).getId().equals(stationsBaseName.get(j).getId())) {
          double dist =
              BuilderUtils.getDistance(stations.get(i), stations.get(j)) / walkSpeed
                  + GraphBuilderApplication.config.graphbuilder.train.getTransferTime();
          vtxList.add(
              new Vertex(
                  stations.get(i).getId(),
                  stations.get(j).getId(),
                  "Walk (Train Interchange)",
                  dist));
          vtxList.add(
              new Vertex(
                  stations.get(j).getId(),
                  stations.get(i).getId(),
                  "Walk (Train Interchange)",
                  dist));
        }
      }
    }
  }

  // Loops in LRTs
  void generateLoops(
      ArrayList<ShpNode> stations,
      HashMap<String, Integer> idxs,
      ArrayList<Vertex> vtxList,
      double mrtSpeed,
      double mrtStopTime,
      double lrtSpeed,
      double lrtStopTime) {
    for (String[] loop : GraphBuilderApplication.config.graphbuilder.train.getLoops()) {
      if (loop.length != 3 && loop.length != 4) {
        log.error("Invalid loop: " + Arrays.toString(loop));
        continue;
      }

      for (String s : loop) if (!Utils.isInService(s)) continue;

      double speed = Utils.isLRT(loop[0]) ? mrtSpeed : lrtSpeed;
      double stopTime = Utils.isLRT(loop[0]) ? mrtStopTime : lrtStopTime;

      TrainServiceName tsn = serviceMap.get(loop[1].substring(0, 2));

      if (loop.length == 3) {
        for (int i = idxs.get(loop[1]) + 1; i <= idxs.get(loop[2]); i++) {
          double dist = BuilderUtils.getDistance(stations.get(i), stations.get(i - 1));
          vtxList.add(
              new Vertex(
                  stations.get(i).getId(), stations.get(i - 1).getId(), tsn.descending, dist));
          vtxList.add(
              new Vertex(
                  stations.get(i - 1).getId(), stations.get(i).getId(), tsn.ascending, dist));
        }
      } else if (loop.length == 4) {
        for (int i = idxs.get(loop[0]) + 1; i <= idxs.get(loop[1]); i++) {
          double dist =
              BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / speed + stopTime;
          vtxList.add(
              new Vertex(
                  stations.get(i).getId(),
                  stations.get(i - 1).getId(),
                  tsn.straightAscending,
                  dist));
          vtxList.add(
              new Vertex(
                  stations.get(i - 1).getId(),
                  stations.get(i).getId(),
                  tsn.straightDescending,
                  dist));
        }

        ShpNode a = stations.get(idxs.get(loop[1]));
        ShpNode b = stations.get(idxs.get(loop[2]));
        double transdist = BuilderUtils.getDistance(a, b) / speed + stopTime;
        vtxList.add(new Vertex(a.getId(), b.getId(), tsn.ascending, transdist));
        vtxList.add(new Vertex(b.getId(), a.getId(), tsn.descending, transdist));

        for (int i = idxs.get(loop[2]) + 1; i <= idxs.get(loop[3]); i++) {
          double dist =
              BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / speed + stopTime;
          vtxList.add(
              new Vertex(
                  stations.get(i).getId(), stations.get(i - 1).getId(), tsn.ascending, dist));
          vtxList.add(
              new Vertex(
                  stations.get(i - 1).getId(), stations.get(i).getId(), tsn.descending, dist));
        }
      }
    }
  }

  static void generateExits(
      ArrayList<ShpNode> stations,
      HashMap<String, Integer> idxs,
      ArrayList<Vertex> vtxList,
      ArrayList<ShpNode> exits,
      HashSet<String> usedExitIds,
      HashSet<String> exitStationIds,
      ArrayList<Node> exitNodeList,
      ArrayList<Node> nodeList,
      double walkSpeed) {
    for (ShpNode exit : exits) {
      exitStationIds.add(exit.getId());

      String[] exitNum = exit.getName().split("EXIT ");
      String exitLetter = "EXIT";
      if (exitNum.length > 1) exitLetter = "EXIT-" + exitNum[1];

      String exitId = String.format("%s-%s", exit.getId(), exitLetter);
      int count = 1;
      String tmpExitId = exitId;
      while (!usedExitIds.add(tmpExitId)) {
        tmpExitId = exitId + "-" + count;
      }
      exitId = tmpExitId;

      Node addedNode = new Node(exitId, exit.getName(), exit.getLat(), exit.getLon());
      exitNodeList.add(addedNode);
      nodeList.add(addedNode);

      double dist =
          BuilderUtils.getDistance(exit, stations.get(idxs.get(exit.getId()))) / walkSpeed;
      vtxList.add(new Vertex(exitId, exit.getId(), "Walk (Exit-Station)", dist));
      vtxList.add(new Vertex(exit.getId(), exitId, "Walk (Station-Exit)", dist));
    }

    // Handle stations without exits
    for (ShpNode s : stations) {
      if (!exitStationIds.contains(s.getId())) {
        String exitId = String.format("%s-EXIT", s.getId());
        Node addedNode = new Node(exitId, s.getName(), s.getLat(), s.getLon());
        exitNodeList.add(addedNode);
        nodeList.add(addedNode);

        vtxList.add(
            new Vertex(
                exitId,
                s.getId(),
                "Walk (Exit-Station)",
                GraphBuilderApplication.config.graphbuilder.train.getTransferTime()));
        vtxList.add(
            new Vertex(
                s.getId(),
                exitId,
                "Walk (Station-Exit)",
                GraphBuilderApplication.config.graphbuilder.train.getTransferTime()));
      }
    }
  }
}

class BuilderUtils {
  static double getDistance(ShpNode a, ShpNode b) {
    TrainGraphBuilder.geoCalc.setStartingGeographicPoint(a.getLon(), a.getLat());
    TrainGraphBuilder.geoCalc.setDestinationGeographicPoint(b.getLon(), b.getLat());
    return (TrainGraphBuilder.geoCalc.getOrthodromicDistance() / 1000.0);
  }

  static TrainServiceName getService(ShpNode a, ShpNode b) {
    if (!TrainGraphBuilder.serviceMap.containsKey(a.getId().substring(0, 2)))
      return TrainGraphBuilder.serviceMap.get(b.getId().substring(0, 2));
    return TrainGraphBuilder.serviceMap.get(a.getId().substring(0, 2));
  }

  static ShpNode getBaseName(ShpNode node) {
    ShpNode tmp = new ShpNode(node);
    tmp.setName(node.getName().split("\\ \\wRT STATION.*")[0]);
    return tmp;
  }
}
