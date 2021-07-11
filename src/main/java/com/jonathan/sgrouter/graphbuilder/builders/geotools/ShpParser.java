package com.jonathan.sgrouter.graphbuilder.builders.geotools;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.utils.Utils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

@Slf4j
public class ShpParser {
  public static ArrayList<ShpNode> parse(
      String filename, boolean trainExit) { // Filename excludes extension
    ArrayList<ShpNode> output = new ArrayList<>();
    try {
      CoordinateReferenceSystem crs =
          CRS.parseWKT(
              new String(Files.readAllBytes(Paths.get(filename + ".prj")), StandardCharsets.UTF_8));

      File shpFile = new File(filename + ".shp");

      DataStore ds = DataStoreFinder.getDataStore(Map.of("url", shpFile.toURI().toURL()));

      FeatureSource<SimpleFeatureType, SimpleFeature> fs =
          ds.getFeatureSource(ds.getTypeNames()[0]);

      FeatureCollection<SimpleFeatureType, SimpleFeature> fc = fs.getFeatures(Filter.INCLUDE);
      FeatureIterator<SimpleFeature> features = fc.features();

      MathTransform mt = CRS.findMathTransform(crs, CRS.parseWKT(Utils.getLatLonWKT()));
      while (features.hasNext()) {
        SimpleFeature feature = features.next();

        Point p = (Point) feature.getAttribute("the_geom");
        Coordinate origP = p.getCoordinate();
        Coordinate result = JTS.transform(origP, origP, mt);

        String ids = (String) feature.getAttribute("STN_NO");
        String name = (String) feature.getAttribute("STN_NAME");

        if (ids.isBlank()) {
          if (GraphBuilderApplication.config
              .graphbuilder
              .train
              .getNameToIds()
              .containsKey(name.toLowerCase()))
            ids =
                GraphBuilderApplication.config
                    .graphbuilder
                    .train
                    .getNameToIds()
                    .get(name.toLowerCase());
          else {
            log.debug(String.format("Unknown station: %s", name));
            continue;
          }
        }
        String[] splitId = ids.split(" / ");
        if (trainExit) {
          if (splitId.length > 1) ids = splitId[0];
          String exitStr = (String) feature.getAttribute("EXIT_CODE");
          if (exitStr == null || exitStr.equals("NULL")) exitStr = " EXIT";
          else exitStr = " EXIT " + exitStr;
          output.add(new ShpNode(ids, name + exitStr, result.y, result.x));
        } else
          for (int i = 0; i < splitId.length; i++)
            output.add(new ShpNode(splitId[i], name, result.y, result.x));

        // log.trace(String.format("%s: %s |
        // %s",feature.getAttribute("STN_NO"),feature.getAttribute("STN_NAME"),feature.getAttribute("the_geom")));
      }
      features.close();
      ds.dispose();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return output;
  }
}
