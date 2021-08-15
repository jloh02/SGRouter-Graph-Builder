package com.jonathan.sgrouter.graphbuilder.utils;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.builders.datamall.TrafficSpeedBand;
import com.jonathan.sgrouter.graphbuilder.models.IndexedBusStopRoad;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.linearref.LinearLocation;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

@Slf4j
public class SpatialIndex {
  private static org.locationtech.jts.index.SpatialIndex idx;

  public static void create(HashMap<Integer, TrafficSpeedBand> speeds) {
    idx = new STRtree();
    try {
      SimpleFeatureType TYPE = DataUtilities.createType("LINESTRING", "geom:LineString");

      WKTReader2 wkt = new WKTReader2();
      for (TrafficSpeedBand t : speeds.values()) {
        if (t.getMaxSpeed() >= 60) t.setMaxSpeed(60);
        if (t.getMinSpeed() >= 60) t.setMinSpeed(60);
        // roads.add(
        //     new Road(
        //         Utils.approxXY(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])),
        //         Utils.approxXY(Double.parseDouble(coords[2]), Double.parseDouble(coords[3])),
        //         (t.getMinSpeed() + t.getMaxSpeed())* 0.5*
        // GraphBuilderApplication.config.graphbuilder.bus.getSpeedFactor()));

        double[] coords =
            Arrays.stream(t.getLocation().split(" ")).mapToDouble(Double::parseDouble).toArray();
        SimpleFeature feat =
            SimpleFeatureBuilder.build(
                TYPE,
                new Object[] {
                  wkt.read(
                      String.format(
                          "LINESTRING(%f %f,%f %f)",
                          coords[1], coords[0], coords[3], coords[2])), // X, Y = Lng, Lat
                },
                null);
        Geometry geom = (LineString) feat.getDefaultGeometry();
        if (geom == null) {
          log.warn("Geometry NULL");
          continue;
        }
        idx.insert(
            geom.getEnvelopeInternal(),
            new IndexedBusStopRoad(
                geom,
                (t.getMinSpeed() + t.getMaxSpeed())
                    * 0.5
                    * GraphBuilderApplication.config.graphbuilder.bus.getSpeedFactor()
                    * 0.0166666666 /* Convert from km/h to km/min*/));
      }

    } catch (SchemaException e) {
      log.error(e.getMessage());
    } catch (ParseException e) {
      log.error(e.getMessage());
    }
  }

  public static double query(double lat, double lng) {
    Coordinate c = new Coordinate(lng, lat);
    Envelope searchBound = new Envelope(c);
    searchBound.expandBy(0.0005);

    double minDist = Double.POSITIVE_INFINITY,
        minDistSpeed = GraphBuilderApplication.config.graphbuilder.bus.getDefaultSpeed();
    for (int i = 0; i < 5; i++) {
      @SuppressWarnings("unchecked")
      List<IndexedBusStopRoad> lines = idx.query(searchBound);
      for (IndexedBusStopRoad line : lines) {
        LinearLocation projectedLine = line.project(c);
        Coordinate pointOnLine = line.extractPoint(projectedLine);
        double dist = pointOnLine.distance(c);
        if (dist < minDist) {
          minDist = dist;
          minDistSpeed = line.getSpeed();
        }
      }
      if (lines.size() != 0) break;
      searchBound.expandBy(0.0001);
    }
    // log.debug("{} {}", minDist, minDistSpeed);
    return minDistSpeed;
  }
}
