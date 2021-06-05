package com.jonathan.sgrouter.graphbuilder.builders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.jonathan.sgrouter.graphbuilder.builders.datamall.DatamallSHP;
import com.jonathan.sgrouter.graphbuilder.builders.geotools.ShpNode;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.models.BranchInfo;
import com.jonathan.sgrouter.graphbuilder.models.Vertex;
import com.jonathan.sgrouter.graphbuilder.models.Node;
import com.jonathan.sgrouter.graphbuilder.models.config.BranchConfig;
import com.jonathan.sgrouter.graphbuilder.models.config.TrainServiceName;
import com.jonathan.sgrouter.graphbuilder.utils.SQLiteHandler;
import com.jonathan.sgrouter.graphbuilder.utils.Utils;

import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;

public class MrtGraphBuilder {
	static GeodeticCalculator geoCalc;
	static Map<String, TrainServiceName> serviceMap = new HashMap<>(
			GraphBuilderApplication.config.graphbuilder.train.getServices());

	public static List<Node> build(SQLiteHandler sqh, double mrtSpeed, double mrtStopTime, double lrtSpeed,
			double lrtStopTime, double walkSpeed) { // MRT speed in km per minute
		List<ShpNode> stations = DatamallSHP.getSHP("TrainStation");
		List<ShpNode> exits = DatamallSHP.getSHP("TrainStationExit");

		stations.removeIf(x->!Utils.isInService(x.getId()));
		exits.removeIf(x->!Utils.isInService(x.getId()));

		Collections.sort(stations);
		Collections.sort(exits);

		//System.out.println(stations);
		//System.out.println(exits);

		/*----------------------Generate train network adjacency list----------------------*/
		Set<String> srcList = new HashSet<>();
		List<Vertex> vtxList = new ArrayList<>();

		List<ShpNode> stationsBaseName = new ArrayList<>();
		Map<String, Integer> idxs = new HashMap<>();
		
		Map<String, BranchInfo> branches = new HashMap<>();

		double freq = Utils.getFreq(GraphBuilderApplication.config.graphbuilder.train.getFreq());
		freq *= 0.5;
		try {
			geoCalc = new GeodeticCalculator(CRS.parseWKT(Utils.getLatLonWKT()));

			setupData(stations, idxs, stationsBaseName, srcList);
			setupBranches(branches, stations, idxs, mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime);
			generateLinesAndBranches(stations, idxs, vtxList, branches, mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime,
					freq);
			generateInterchanges(stations, stationsBaseName, vtxList, walkSpeed);
			generateLoops(stations, idxs, vtxList, mrtSpeed, mrtStopTime, lrtSpeed, lrtStopTime, freq);
		} catch (FactoryException e) {
			System.err.println(e);
		}
		/*----------------------Generate output: Station coordinates list----------------------*/
		List<Node> nodeList = new ArrayList<>();
		for (String src : srcList) {
			ShpNode stationData = stations.get(idxs.get(src));
			nodeList.add(new Node(src, stationData.getName(), stationData.getLat(), stationData.getLon()));
		}

		/*----------------------Handle Exit vertices while adding to Station Exit coordinates list----------------------*/
		Set<String> usedExitIds = new HashSet<>();
		Set<String> exitStationIds = new HashSet<>();
		List<Node> exitNodeList = new ArrayList<>();

		generateExits(stations, idxs, vtxList, exits, usedExitIds, exitStationIds, exitNodeList, nodeList, walkSpeed);

		/*----------------------Update DB----------------------*/
		sqh.addNodes(nodeList);
		sqh.addVertices(vtxList);

		return exitNodeList;
	}

	//Branch setup (Refer to branch-logic.txt)
	static void setupBranches(Map<String, BranchInfo> branches, List<ShpNode> stations, Map<String, Integer> idxs, double mrtSpeed,
			double mrtStopTime, double lrtSpeed, double lrtStopTime) {
		for (BranchConfig bc : GraphBuilderApplication.config.graphbuilder.train.getBranches()) {
			double sum = 0;
			List<Double> cumu = new ArrayList<>();
			List<String> branchNodes = new ArrayList<>();
			cumu.add(0.0);
			if(!idxs.containsKey(bc.getSrc())||!idxs.containsKey(bc.getDes())) continue;
			branchNodes.add(stations.get(idxs.get(bc.getSrc())).getId());
			for (int i = idxs.get(bc.getSrc()) + 1; i <= idxs.get(bc.getDes()); i++) {
				if (!Utils.isLRT(bc.getSrc()))
					sum += BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / mrtSpeed + mrtStopTime;
				else
					sum += BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / lrtSpeed + lrtStopTime;
				cumu.add(sum);
				branchNodes.add(stations.get(i).getId());
			}
			branches.put(bc.getBranchNode(), new BranchInfo(cumu, branchNodes, bc.getTransferTime(), bc.isJoin(),
					bc.getPostBranchService(), bc.getBranchService()));
			if (!bc.isJoin())
				throw new UnsupportedOperationException("Split branches not implemented yet");
		}
	}

	static void setupData(List<ShpNode> stations, Map<String, Integer> idxs, List<ShpNode> stationsBaseName,
			Set<String> srcList) {
		for (int i = 0; i < stations.size(); i++) {
			idxs.put(stations.get(i).getId(), i);
			srcList.add(stations.get(i).getId());
			stationsBaseName.add(BuilderUtils.getBaseName(stations.get(i)));
		}
	}

	//Linear Train Vertex Creation: Generates vertices on graph which are "straight lines"
	//Branch Creation: Refer to branch-logic.txt
	static void generateLinesAndBranches(List<ShpNode> stations, Map<String, Integer> idxs, List<Vertex> vtxList,
			Map<String, BranchInfo> branches, double mrtSpeed, double mrtStopTime, double lrtSpeed, double lrtStopTime,
			double freq) {
		int startIdx = 0;
		double sumDist = 0.0;

		BranchInfo currBranch = null;
		double preBranchDistance = 0;

		List<Double> cumDist = new ArrayList<>(); //Cumulative sum array to calculate distance between 2 points
		cumDist.add(0.0);
		while (stations.get(startIdx).getId()
				.matches(GraphBuilderApplication.config.graphbuilder.train.getExcludeLine()) && startIdx < stations.size())
			startIdx++;
		for (int i = startIdx + 1; i < stations.size(); i++) {
			//Exclude loops from linear vertex creation
			if (stations.get(i).getId().matches(GraphBuilderApplication.config.graphbuilder.train.getExcludeLine())) {
				while (stations.get(i + 1).getId()
						.matches(GraphBuilderApplication.config.graphbuilder.train.getExcludeLine()) && i+1 < stations.size())
					i++;
				startIdx = i + 1;
				sumDist = 0.0;
				cumDist = new ArrayList<>();
				cumDist.add(0.0);
				currBranch = null;
				i++;
				continue;
			}

			if (branches.containsKey(stations.get(i).getId())) {
				currBranch = branches.get(stations.get(i).getId());
				preBranchDistance = sumDist;
			}

			//Add vertex if same lines, else reset line
			if (stations.get(i).getId().substring(0, 2).equals(stations.get(startIdx).getId().substring(0, 2))) {

				double edgeDist = BuilderUtils.getDistance(stations.get(i - 1), stations.get(i));
				// System.out.println(String.format("%s %s %f %f", stations.get(i - 1).getId(),
				// 		stations.get(i).getId(), edgeDist, edgeDist / mrtSpeed + mrtStopTime));
				if (Utils.isLRT(stations.get(i).getId()))
					sumDist += edgeDist / lrtSpeed + lrtStopTime;
				else
					sumDist += edgeDist / mrtSpeed + mrtStopTime;

				cumDist.add(sumDist);
				//System.out.println(cumDist); 

				if (GraphBuilderApplication.config.graphbuilder.train.getInvalidStations()
						.contains(stations.get(i).getId()))
					continue;
				for (int j = startIdx; j < i; j++) {
					if (GraphBuilderApplication.config.graphbuilder.train.getInvalidStations()
							.contains(stations.get(j).getId()))
						continue;

					double dist = cumDist.get(i - startIdx) - cumDist.get(j - startIdx) + freq;
					TrainServiceName serv = currBranch == null ? BuilderUtils.getService(stations.get(i), stations.get(j))
							: currBranch.getPostBranchService();
					vtxList.add(new Vertex(stations.get(i).getId(), stations.get(j).getId(), serv.descending, dist));
					vtxList.add(new Vertex(stations.get(j).getId(), stations.get(i).getId(), serv.ascending, dist));
				}

				if (currBranch != null && currBranch.isJoin()) {
					for (int j = 0; j < currBranch.cumDist.size(); j++) {
						if (GraphBuilderApplication.config.graphbuilder.train.getInvalidStations()
								.contains(stations.get(j).getId()))
							continue;

						double dist = currBranch.cumDist.get(j) + sumDist - preBranchDistance + freq
								+ currBranch.getTransferTime();
						// System.out.println(String.format("%s %s %f %f", stations.get(i).getId(),
						// 		stations.get(idxs.get(currBranch.nodes.get(j))).getId(), currBranch.cumDist.get(j),
						// 		sumDist - preBranchDistance));
						TrainServiceName serv = currBranch.getBranchService();
						vtxList.add(new Vertex(stations.get(i).getId(),
								stations.get(idxs.get(currBranch.nodes.get(j))).getId(), serv.descending, dist));
						vtxList.add(new Vertex(stations.get(idxs.get(currBranch.nodes.get(j))).getId(),
								stations.get(i).getId(), serv.ascending, dist));
					}
				}
			} else {
				startIdx = i;
				sumDist = 0.0;
				cumDist = new ArrayList<>();
				cumDist.add(0.0);
				currBranch = null;
				//System.out.println(stations.get(startIdx));
			}

		}
	}

	//Cross Train Lines Interchanges
	static void generateInterchanges(List<ShpNode> stations, List<ShpNode> stationsBaseName, List<Vertex> vtxList,
			double walkSpeed) {
		for (int i = 1; i < stationsBaseName.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (stationsBaseName.get(i).getName().equals(stationsBaseName.get(j).getName())
						&& !stationsBaseName.get(i).getId().equals(stationsBaseName.get(j).getId())) {
					// System.out.println(stationsBaseName.get(i).getName() + " "
					// 		+ BuilderUtils.getDistance(stations.get(i), stations.get(j)) + " " + walkSpeed);
					double dist = BuilderUtils.getDistance(stations.get(i), stations.get(j)) / walkSpeed
							+ GraphBuilderApplication.config.graphbuilder.train.getTransferTime();
					vtxList.add(new Vertex(stations.get(i).getId(), stations.get(j).getId(),
							"Walk (Between Stations)", dist));
					vtxList.add(new Vertex(stations.get(j).getId(), stations.get(i).getId(),
							"Walk (Between Stations)", dist));
				}
			}
		}
	}

	//Loops in LRTs
	static void generateLoops(List<ShpNode> stations, Map<String, Integer> idxs, List<Vertex> vtxList,
			double mrtSpeed, double mrtStopTime, double lrtSpeed, double lrtStopTime, double freq) {
		for (String[] loop : GraphBuilderApplication.config.graphbuilder.train.getLoops()) {
			if (loop.length != 3 && loop.length != 4) {
				System.err.println("Invalid loop: " + Arrays.toString(loop));
				continue;
			}

			for(String s: loop) if(!Utils.isInService(s)) continue;

			double speed = Utils.isLRT(loop[0]) ? mrtSpeed : lrtSpeed;
			double stopTime = Utils.isLRT(loop[0]) ? mrtStopTime : lrtStopTime;

			TrainServiceName tsn = serviceMap.get(loop[1].substring(0, 2));

			if (loop.length == 3) {

				//Build loop node list and cumulative sum array
				List<ShpNode> nodes = new ArrayList<>();
				nodes.add(stations.get(idxs.get(loop[0])));
				double sumDist = 0.0;
				List<Double> cumDist = new ArrayList<>();
				cumDist.add(0.0);
				sumDist += BuilderUtils.getDistance(stations.get(idxs.get(loop[0])), stations.get(idxs.get(loop[1]))) / speed
						+ stopTime;
				cumDist.add(sumDist);
				nodes.add(stations.get(idxs.get(loop[1])));
				for (int i = idxs.get(loop[1]) + 1; i <= idxs.get(loop[2]); i++) {
					sumDist += BuilderUtils.getDistance(stations.get(i), stations.get(i - 1));
					cumDist.add(sumDist);
					nodes.add(stations.get(i));
				}
				sumDist += BuilderUtils.getDistance(stations.get(idxs.get(loop[0])), stations.get(idxs.get(loop[2]))) / speed
						+ stopTime;

				//System.out.println(nodes);

				//Use cumulative sum array for forward and backward loop traversal (Take lesser)
				double halfLoopDist = sumDist * 0.5;
				for (int i = 1; i < nodes.size(); i++) {
					for (int j = 0; j < i; j++) {
						double dist = cumDist.get(i) - cumDist.get(j);
						if (dist > halfLoopDist) {
							// System.out.println(String.format("%s %s %f %f (Exception)", nodes.get(j).getId(),
							// 		nodes.get(i).getId(), dist, sumDist - dist));
							dist = sumDist - dist;
						}
						// else
						// 	System.out.println(String.format("%s %s %f %f", nodes.get(j).getId(), nodes.get(i).getId(),
						// 			dist, sumDist - dist));
						vtxList.add(
								new Vertex(nodes.get(i).getId(), nodes.get(j).getId(), tsn.descending, dist + freq));
						vtxList.add(
								new Vertex(nodes.get(j).getId(), nodes.get(i).getId(), tsn.ascending, dist + freq));
					}
				}
			} else if (loop.length == 4) {
				//Build straight node list and cumulative sum array
				List<ShpNode> straightNodes = new ArrayList<>();

				double sumDist = 0.0;
				List<Double> straightCumDist = new ArrayList<>();
				straightCumDist.add(0.0);
				straightNodes.add(stations.get(idxs.get(loop[0])));
				for (int i = idxs.get(loop[0]) + 1; i <= idxs.get(loop[1]); i++) {
					sumDist += BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / speed + stopTime;

					straightCumDist.add(sumDist);
					straightNodes.add(stations.get(i));
				}
				double straightSumDist = sumDist;

				//Draw vertices for straight portion
				for (int i = 0; i < straightNodes.size() - 1; i++) {
					for (int j = i + 1; j < straightNodes.size(); j++) {
						double dist = straightCumDist.get(j) - straightCumDist.get(i);
						vtxList.add(new Vertex(straightNodes.get(i).getId(), straightNodes.get(j).getId(),
								tsn.straightAscending, dist));
						vtxList.add(new Vertex(straightNodes.get(j).getId(), straightNodes.get(i).getId(),
								tsn.straightDescending, dist));
					}
				}

				//Build loop node list and cumulative sum array
				List<ShpNode> loopNodes = new ArrayList<>();

				loopNodes.add(stations.get(idxs.get(loop[1])));
				sumDist = 0.0;
				List<Double> loopCumDist = new ArrayList<>();
				loopCumDist.add(0.0);
				sumDist += BuilderUtils.getDistance(stations.get(idxs.get(loop[1])), stations.get(idxs.get(loop[2]))) / speed
						+ stopTime;
				loopCumDist.add(sumDist);
				loopNodes.add(stations.get(idxs.get(loop[2])));

				for (int i = idxs.get(loop[2]) + 1; i <= idxs.get(loop[3]); i++) {
					sumDist += BuilderUtils.getDistance(stations.get(i), stations.get(i - 1)) / speed + stopTime;
					loopCumDist.add(sumDist);
					loopNodes.add(stations.get(i));
				}
				sumDist += BuilderUtils.getDistance(stations.get(idxs.get(loop[3])), stations.get(idxs.get(loop[1]))) / speed
						+ stopTime;

				//Use cumulative sum array for forward and backward loop traversal (Take lesser)
				double halfLoopDist = sumDist * 0.5;
				for (int i = 0; i < loopNodes.size(); i++) {
					String servInc = tsn.ascending, servDec = tsn.descending;
					for (int j = i + 1; j < loopNodes.size(); j++) {
						double dist = loopCumDist.get(j) - loopCumDist.get(i);
						//Only applicable for straight-loop vertices
						if (i == 0 && dist > halfLoopDist) {
							dist = sumDist - dist;
							servInc = tsn.descending;
							servDec = tsn.ascending;
						}

						vtxList.add(
								new Vertex(loopNodes.get(i).getId(), loopNodes.get(j).getId(), servInc, dist + freq));
						vtxList.add(
								new Vertex(loopNodes.get(j).getId(), loopNodes.get(i).getId(), servDec, dist + freq));

						//Join loops to straight
						if (i == 0) { // loop[1]

							for (int k = 0; k < straightNodes.size() - 1; k++) { //Last straight node belongs to loop
								double straightDist = straightSumDist - straightCumDist.get(k);
								// System.out.println(String.format("%s %s %f %f", loopNodes.get(j).getId(),
								// 		straightNodes.get(k).getId(), dist, straightDist));
								vtxList.add(new Vertex(loopNodes.get(j).getId(), straightNodes.get(k).getId(),
										servDec, dist + straightDist + freq));
								vtxList.add(new Vertex(straightNodes.get(k).getId(), loopNodes.get(j).getId(),
										servInc, dist + straightDist + freq));
							}
						}
					}
				}

			}
		}
	}

	static void generateExits(List<ShpNode> stations, Map<String, Integer> idxs, List<Vertex> vtxList,
			List<ShpNode> exits, Set<String> usedExitIds, Set<String> exitStationIds, List<Node> exitNodeList,
			List<Node> nodeList, double walkSpeed) {
		for (ShpNode exit : exits) {
			exitStationIds.add(exit.getId());

			String[] exitNum = exit.getName().split("EXIT ");
			String exitLetter = "EXIT";
			if (exitNum.length > 1)
				exitLetter = "EXIT-" + exitNum[1];

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

			double dist = BuilderUtils.getDistance(exit, stations.get(idxs.get(exit.getId()))) / walkSpeed;
			vtxList.add(new Vertex(exitId, exit.getId(), "Walk (Station-Exit)", dist));
			vtxList.add(new Vertex(exit.getId(), exitId, "Walk (Station-Exit)", dist));
		}

		//Handle stations without exits
		for (ShpNode s : stations) {
			if (!exitStationIds.contains(s.getId())) {
				String exitId = String.format("%s-EXIT", s.getId());
				Node addedNode = new Node(exitId, s.getName(), s.getLat(), s.getLon());
				exitNodeList.add(addedNode);
				nodeList.add(addedNode);

				vtxList.add(new Vertex(exitId, s.getId(), "Walk (Station-Exit)",
						GraphBuilderApplication.config.graphbuilder.train.getTransferTime()));
				vtxList.add(new Vertex(s.getId(), exitId, "Walk (Station-Exit)",
						GraphBuilderApplication.config.graphbuilder.train.getTransferTime()));
			}
		}
	}
}

class BuilderUtils{
	static double getDistance(ShpNode a, ShpNode b) {
		MrtGraphBuilder.geoCalc.setStartingGeographicPoint(a.getLon(), a.getLat());
		MrtGraphBuilder.geoCalc.setDestinationGeographicPoint(b.getLon(), b.getLat());
		return (MrtGraphBuilder.geoCalc.getOrthodromicDistance() / 1000.0);
	}

	static TrainServiceName getService(ShpNode a, ShpNode b) {
		if (!MrtGraphBuilder.serviceMap.containsKey(a.getId().substring(0, 2)))
			return MrtGraphBuilder.serviceMap.get(b.getId().substring(0, 2));
		return MrtGraphBuilder.serviceMap.get(a.getId().substring(0, 2));
	}

	static ShpNode getBaseName(ShpNode node) {
		ShpNode tmp = new ShpNode(node);
		tmp.setName(node.getName().split("\\ \\wRT STATION.*")[0]);
		return tmp;
	}
}
