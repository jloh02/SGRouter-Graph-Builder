package com.jonathan.sgrouter.graphbuilder.builders.gmap;

import java.io.IOException;
import java.util.List;

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import com.google.maps.model.TransitMode;
import com.google.maps.model.TransitRoutingPreference;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;

public class GmapConnection {
	GeoApiContext ctx;

	public GmapConnection() {
		//System.out.println(gcfg.getApiKey());
		ctx = new GeoApiContext.Builder() //On deployment, change to: new GeoApiContext.Builder(new GaeRequestHandler.Builder())
				.apiKey(GraphBuilderApplication.config.gmap.getApiKey()).build();
	}

	public double getWalkSpeed(LatLng src, LatLng des) throws IOException, InterruptedException, ApiException {
		DirectionsResult res = DirectionsApi.newRequest(ctx).mode(TravelMode.WALKING).units(Unit.METRIC)
				.alternatives(false).origin(src).destination(des).await();

		System.out.println((double) res.routes[0].legs[0].distance.inMeters
				/ (double) res.routes[0].legs[0].duration.inSeconds * 0.06);
		// System.out.println(String.format("%f %f %f",(double) res.routes[0].legs[0].distance.inMeters,(double) res.routes[0].legs[0].duration.inSeconds,(double) res.routes[0].legs[0].distance.inMeters / (double) res.routes[0].legs[0].duration.inSeconds* 0.06));
		return (double) res.routes[0].legs[0].distance.inMeters / (double) res.routes[0].legs[0].duration.inSeconds
				* 0.06;
	}

	public GmapTiming getGmapSpeed(String srcPID, String desPID, TransitMode mode, String serviceSearchStr,
			GmapTiming defaultTiming) {
		GmapData gdA = getTimingWithPid(srcPID, desPID, mode, serviceSearchStr);
		//System.out.println(gdA);

		if (gdA.isNull())
			return new GmapTiming(defaultTiming.speed, defaultTiming.stopTime);

		int jump = (int) (gdA.polyline.size() * 0.05);
		int maxJump = (int) (gdA.polyline.size() * 0.5);
		for (int i = 2; jump * i <= maxJump; i++) { //Between 50-90% of journey for 2nd destination node
			GmapData gdB = getTimingWithLatLng(srcPID, gdA.polyline.get(gdA.polyline.size() - 1 - jump * i), mode,
					serviceSearchStr);
			//System.out.println(gdB);

			// Calculate stop time (Refer to MD file "gmap_stoptime_and_speed_calculation" for explanation)		
			double sRatio = (double) gdB.stops / (double) gdA.stops;

			if (gdB.isNull() || gdB.stops == gdA.stops || (gdB.time - sRatio * gdA.time) == 0)
				continue;

			double speed = (gdB.distance - sRatio * gdA.distance) / (gdB.time - sRatio * gdA.time);
			double stopTime = (gdA.time * speed - gdA.distance) / ((double) gdA.stops * speed);

			// If speed/stopTime <= 0, timing/distance does not change with number of stops, hence stopTime is insignificant
			if (speed <= 0 || stopTime <= 0) {
				double speedA = gdA.distance / gdA.time;
				double speedB = gdA.distance / gdB.time;
				return new GmapTiming((speedA + speedB) * 0.5, 0);
			}

			return new GmapTiming(speed, stopTime);
		}

		return new GmapTiming(gdA.distance / (gdA.time - gdA.stops * defaultTiming.stopTime), defaultTiming.stopTime);
	}

	private GmapData getTimingWithLatLng(String srcPID, LatLng desLatLng, TransitMode mode, String searchStr) {
		DirectionsApiRequest req = DirectionsApi.newRequest(ctx).mode(TravelMode.TRANSIT).units(Unit.METRIC)
				.alternatives(true).transitMode(mode).transitRoutingPreference(TransitRoutingPreference.FEWER_TRANSFERS)
				.originPlaceId(srcPID).destination(desLatLng);
		return getTimingAbstract(req, mode, searchStr);
	}

	private GmapData getTimingWithPid(String srcPID, String desPID, TransitMode mode, String searchStr) {
		DirectionsApiRequest req = DirectionsApi.newRequest(ctx).mode(TravelMode.TRANSIT).units(Unit.METRIC)
				.alternatives(true).transitMode(mode).transitRoutingPreference(TransitRoutingPreference.FEWER_TRANSFERS)
				.originPlaceId(srcPID).destinationPlaceId(desPID);
		return getTimingAbstract(req, mode, searchStr);
	}

	private GmapData getTimingAbstract(DirectionsApiRequest req, TransitMode mode, String searchStr) {
		try {
			DirectionsResult res = req.await();
			// System.out.println("----------");
			for (DirectionsRoute route : res.routes) {
				DirectionsLeg leg = route.legs[0];

				// System.out.println(leg);
				// System.out.println(leg.steps.length);
				// System.out.println(Arrays.toString(leg.steps));

				if (leg.steps.length > 3)
					continue;

				int stepIdx = leg.steps.length / 2;
				if (leg.steps.length == 2 && leg.steps[0].travelMode == TravelMode.TRANSIT)
					stepIdx = 0;

				//System.out.println(leg.steps[stepIdx].transitDetails);

				if (!leg.steps[stepIdx].transitDetails.line.toString().contains(searchStr)
						&& !leg.steps[stepIdx].transitDetails.headsign.toString().contains(searchStr))
					continue;

				// System.out.println(leg.steps[0].distance.inMeters);
				// System.out.println(leg.steps[0].duration.inSeconds);

				// System.out.println("----------");

				return new GmapData(leg.steps[stepIdx].transitDetails.numStops,
						(double) leg.steps[stepIdx].distance.inMeters / 1000.0,
						(double) leg.steps[stepIdx].duration.inSeconds / 60.0,
						leg.steps[stepIdx].polyline.decodePath());

			}
		} catch (IOException e) {
			System.out.println(e);
		} catch (ApiException e) {
			System.out.println(e);
		} catch (InterruptedException e) {
			System.out.println(e);
		}
		return new GmapData();
	}

	public void close() {
		ctx.shutdown();
	}

	@Deprecated
	public GmapTiming getTwoDesGmapSpeed(String srcPID, String desPID_1, String desPID_2, TransitMode mode,
			String serviceSearchStr, double defaultSpeed, double defaultStop) {
		GmapData gdA = getTimingWithPid(srcPID, desPID_1, mode, serviceSearchStr);
		//System.out.println(gdA);

		GmapData gdB = getTimingWithPid(srcPID, desPID_2, mode, serviceSearchStr);
		//System.out.println(gdB);

		if (gdA.isNull() && gdB.isNull())
			return new GmapTiming(defaultSpeed, defaultStop);

		// Calculate stop time (Refer to MD file "gmap_stoptime_and_speed_calculation" for explanation)
		if (!gdA.isNull() && !gdB.isNull()) {
			double sRatio = (double) gdB.stops / (double) gdA.stops;
			double speedDenom = (gdB.time - sRatio * gdA.time);
			if (speedDenom == 0) {
				double speedA = gdA.distance / (gdA.time - gdA.stops * defaultStop);
				double speedB = gdA.distance / (gdB.time - gdB.stops * defaultStop);
				return new GmapTiming((speedA + speedB) / 2, defaultStop);
			}
			double speed = (gdB.distance - sRatio * gdA.distance) / speedDenom;
			double stopTime = (gdA.time * speed - gdA.distance) / ((double) gdA.stops * speed);
			return new GmapTiming(speed, stopTime);
		}

		GmapData gd = gdB.isNull() ? gdA : gdB;
		return new GmapTiming(gd.distance / (gd.time - gd.stops * defaultStop), defaultStop);
	}
}

class GmapData {
	public int stops = -1;
	public List<LatLng> polyline = null;
	public double distance, time;

	public GmapData() {
	}

	public GmapData(int stops, double distance, double time) {
		this.stops = stops;
		this.distance = distance;
		this.time = time;
	}

	public GmapData(int stops, double distance, double time, List<LatLng> polyline) {
		this.stops = stops;
		this.distance = distance;
		this.time = time;
		this.polyline = polyline;
	}

	public boolean isNull() {
		return stops == -1 || polyline == null;
	}

	@Override
	public String toString() {
		return String.format("{stops=%d, distance=%f km, time=%f min}", stops, distance, time);
	}
}
