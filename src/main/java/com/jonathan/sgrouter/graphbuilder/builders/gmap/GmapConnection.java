package com.jonathan.sgrouter.graphbuilder.builders.gmap;

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
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GmapConnection {
  static GeoApiContext ctx =
      new GeoApiContext.Builder().apiKey(GraphBuilderApplication.config.gmap.getApiKey()).build();

  public double getWalkSpeed(LatLng src, LatLng des)
      throws IOException, InterruptedException, ApiException {
    DirectionsResult res =
        DirectionsApi.newRequest(ctx)
            .mode(TravelMode.WALKING)
            .units(Unit.METRIC)
            .alternatives(false)
            .origin(src)
            .destination(des)
            .await();

    double walkSpeed =
        (double) res.routes[0].legs[0].distance.inMeters
            / (double) res.routes[0].legs[0].duration.inSeconds
            * 0.06;
    log.debug("Walk speed: {}", walkSpeed);
    return walkSpeed;
  }

  public GmapTiming getGmapSpeed(
      String srcPID,
      String desPID,
      TransitMode mode,
      String serviceSearchStr,
      GmapTiming defaultTiming) {
    GmapData gdA = getTimingWithPid(srcPID, desPID, mode, serviceSearchStr);
    // log.trace(gdA.toString());

    if (gdA.isNull()) return new GmapTiming(defaultTiming.speed, defaultTiming.stopTime);

    int jump = (int) (gdA.polyline.size() * 0.05);
    int maxJump = (int) (gdA.polyline.size() * 0.5);
    for (int i = 2;
        jump * i <= maxJump;
        i++) { // Between 50-90% of journey for 2nd destination node
      GmapData gdB =
          getTimingWithLatLng(
              srcPID, gdA.polyline.get(gdA.polyline.size() - 1 - jump * i), mode, serviceSearchStr);
      // log.trace(gdB.toString());

      // Calculate stop time (Refer to MD file "gmap_stoptime_and_speed_calculation" for
      // explanation)
      double sRatio = (double) gdB.stops / (double) gdA.stops;

      if (gdB.isNull() || gdB.stops == gdA.stops || (gdB.time - sRatio * gdA.time) == 0) continue;

      double speed = (gdB.distance - sRatio * gdA.distance) / (gdB.time - sRatio * gdA.time);
      double stopTime = (gdA.time * speed - gdA.distance) / ((double) gdA.stops * speed);

      // If speed/stopTime <= 0, timing/distance does not change with number of stops, hence
      // stopTime is insignificant
      if (speed <= 0 || stopTime <= 0) {
        double speedA = gdA.distance / gdA.time;
        double speedB = gdA.distance / gdB.time;
        return new GmapTiming((speedA + speedB) * 0.5, 0);
      }

      return new GmapTiming(speed, stopTime);
    }

    return new GmapTiming(
        gdA.distance / (gdA.time - gdA.stops * defaultTiming.stopTime), defaultTiming.stopTime);
  }

  private GmapData getTimingWithLatLng(
      String srcPID, LatLng desLatLng, TransitMode mode, String searchStr) {
    DirectionsApiRequest req =
        DirectionsApi.newRequest(ctx)
            .mode(TravelMode.TRANSIT)
            .units(Unit.METRIC)
            .alternatives(true)
            .transitMode(mode)
            .transitRoutingPreference(TransitRoutingPreference.FEWER_TRANSFERS)
            .originPlaceId(srcPID)
            .destination(desLatLng);
    return getTimingAbstract(req, mode, searchStr);
  }

  private GmapData getTimingWithPid(
      String srcPID, String desPID, TransitMode mode, String searchStr) {
    DirectionsApiRequest req =
        DirectionsApi.newRequest(ctx)
            .mode(TravelMode.TRANSIT)
            .units(Unit.METRIC)
            .alternatives(true)
            .transitMode(mode)
            .transitRoutingPreference(TransitRoutingPreference.FEWER_TRANSFERS)
            .originPlaceId(srcPID)
            .destinationPlaceId(desPID);
    return getTimingAbstract(req, mode, searchStr);
  }

  private GmapData getTimingAbstract(DirectionsApiRequest req, TransitMode mode, String searchStr) {
    try {
      DirectionsResult res = req.await();
      // log.trace("----------");
      for (DirectionsRoute route : res.routes) {
        DirectionsLeg leg = route.legs[0];

        // log.trace(leg);
        // log.trace(leg.steps.length);
        // log.trace(Arrays.toString(leg.steps));

        if (leg.steps.length > 3) continue;

        int stepIdx = leg.steps.length / 2;
        if (leg.steps.length == 2 && leg.steps[0].travelMode == TravelMode.TRANSIT) stepIdx = 0;

        // log.trace(leg.steps[stepIdx].transitDetails);

        if (!leg.steps[stepIdx].transitDetails.line.toString().contains(searchStr)
            && !leg.steps[stepIdx].transitDetails.headsign.toString().contains(searchStr)) continue;

        // log.trace(leg.steps[0].distance.inMeters);
        // log.trace(leg.steps[0].duration.inSeconds);

        // log.trace("----------");

        return new GmapData(
            leg.steps[stepIdx].transitDetails.numStops,
            (double) leg.steps[stepIdx].distance.inMeters / 1000.0,
            (double) leg.steps[stepIdx].duration.inSeconds / 60.0,
            leg.steps[stepIdx].polyline.decodePath());
      }
    } catch (IOException e) {
      log.error(e.getMessage());
    } catch (ApiException e) {
      log.error(e.getMessage());
    } catch (InterruptedException e) {
      log.error(e.getMessage());
    }
    return new GmapData();
  }

  public static void close() {
    ctx.shutdown();
  }
}

class GmapData {
  public int stops = -1;
  public List<LatLng> polyline = null;
  public double distance, time;

  public GmapData() {}

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
