package com.jonathan.sgrouter.graphbuilder.builders;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import java.time.ZonedDateTime;
import java.time.DayOfWeek;

import com.jonathan.sgrouter.graphbuilder.utils.*;
import com.jonathan.sgrouter.graphbuilder.builders.datamall.*;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.models.DBVertex;
import com.jonathan.sgrouter.graphbuilder.models.Node;

public class BusGraphBuilder {
	public static List<Node> build(SQLiteHandler sqh, double busSpeed, double busStopTime) { // Bus speed in km per minute
		/*----------------------Import data from Datamall----------------------*/
		Map<String, BusStop> importedBusStops = new BusStopData().getBusData();
		Map<BusServiceKey, BusService> importedBusServices = new BusServiceData().getBusData();
		Map<BusRouteKey, BusRoute> importedBusRoutes = new BusRouteData().getBusData();

		// System.out.println(importedBusStops.toString().substring(0,1000));
		// System.out.println(importedBusServices.toString().substring(0,1000));
		// System.out.println(importedBusRoutes.toString().substring(0,1000));

		// System.out.println("\n----------------\nNon-matching bus stops:");
		// for (String s : importedBusStops.keySet()) if(!s.matches("^\\d{5}"))
		// System.out.println(s);

		// System.out.println("----------------\nNon-matching bus services:");
		// for (BusRouteKey s : importedBusRoutes.keySet())
		//
		// System.out.println("----------------");
		// for (BusServiceKey s : importedBusServices.keySet())
		// if(!s.service.matches("^(\\d{1,3}[ABCEGMNRWXe]?)|(NR\\d)|(CT8)|(CT18)"))
		// System.out.println(s);
		// System.out.println("----------------");

		/*----------------------Generate list of sorted keys for iteration----------------------*/
		List<String> sortedBusStops = new ArrayList<>(importedBusStops.keySet());
		Collections.sort(sortedBusStops);
		List<BusServiceKey> sortedBusServices = new ArrayList<>(importedBusServices.keySet());
		Collections.sort(sortedBusServices);
		List<BusRouteKey> sortedBusRoutes = new ArrayList<>(importedBusRoutes.keySet());
		Collections.sort(sortedBusRoutes);

		// System.out.println(sortedBusStops.toString().substring(0, 100));
		// System.out.println(sortedBusServices.toString().substring(0, 100));
		// System.out.println(sortedBusRoutes.toString().substring(0, 1000));

		/*----------------------Generate bus adjacency list----------------------*/
		Set<String> srcList = new HashSet<>();
		List<DBVertex> vtxList = new ArrayList<>();

		ZonedDateTime sgNow = GraphBuilderApplication.sgNow;
		
		for (int i = 0; i < sortedBusRoutes.size(); i++) {
			BusRouteKey srcRouteKey = sortedBusRoutes.get(i);
			BusRoute srcRouteData = importedBusRoutes.get(srcRouteKey);
			BusService srcServiceData = importedBusServices.get(BusServiceKey.fromBusRouteKey(srcRouteKey));

			//if(srcRouteKey.service.equals("11")) System.out.println(String.format("%s %s",srcRouteKey,srcRouteData));

			//Handles abnormal cases such as CTE expressway as a "node"
			if (!Utils.isBusStop(srcRouteData.src)) 
				continue;

			srcList.add(srcRouteData.src);

			// System.out.println(srcRouteData);
			// System.out.println(serviceData);

			// Check if operational
			ZonedDateTime firstBus, lastBus;
			if (sgNow.getDayOfWeek() == DayOfWeek.SATURDAY) {
				if (srcRouteData.SAT_first.equals("-"))
					continue;
				firstBus = getFirstLastBusDT(sgNow, srcRouteData.SAT_first);
				lastBus = getFirstLastBusDT(sgNow, srcRouteData.SAT_last);
			} else if (sgNow.getDayOfWeek() == DayOfWeek.SUNDAY) {
				if (srcRouteData.SUN_first.equals("-"))
					continue;
				firstBus = getFirstLastBusDT(sgNow, srcRouteData.SUN_first);
				lastBus = getFirstLastBusDT(sgNow, srcRouteData.SUN_last);
			} else {
				if (srcRouteData.WD_first.equals("-"))
					continue;
				firstBus = getFirstLastBusDT(sgNow, srcRouteData.WD_first);
				lastBus = getFirstLastBusDT(sgNow, srcRouteData.WD_last);
			}
			if (sgNow.isBefore(firstBus) || sgNow.isAfter(lastBus))
				continue;

			double freq = Utils.getFreq(srcServiceData.getFreqArr());

			for (int j = i + 1; j < sortedBusRoutes.size() && sortedBusRoutes.get(j).direction == srcRouteKey.direction
					&& sortedBusRoutes.get(j).service.equals(srcRouteKey.service); j++) {
				BusRouteKey desRouteKey = sortedBusRoutes.get(j);
				BusRoute desRouteData = importedBusRoutes.get(desRouteKey);
				if (!Utils.isBusStop(desRouteData.src))
					continue;
				double travelTime = (desRouteData.distance - srcRouteData.distance) / busSpeed + freq;

				vtxList.add(new DBVertex(srcRouteData.src, desRouteData.src, srcRouteKey.service, travelTime));
			}
		}

		/*----------------------Generate output: Bus coordinates list----------------------*/
		List<Node> nodeList = new ArrayList<>();
		for (String src : srcList) {
			BusStop bsData = importedBusStops.get(src);
			nodeList.add(new Node(src, bsData.description, bsData.lat, bsData.lon));
		}

		/*----------------------Update DB----------------------*/
		sqh.addNodes(nodeList);
		sqh.addVertices(vtxList);
		return nodeList;
	}

	static ZonedDateTime getFirstLastBusDT(ZonedDateTime now, String fl_bus) {
		if (fl_bus.equals("2400"))
			fl_bus = "2359";
		return now.withHour(Integer.parseInt(fl_bus.substring(0, 2))).withMinute(Integer.parseInt(fl_bus.substring(2)))
				.withSecond(0);
	}
}
