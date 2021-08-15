package com.jonathan.sgrouter.graphbuilder.utils;

import java.time.ZonedDateTime;
import org.locationtech.jts.geom.Coordinate;

public class Utils {

  public static boolean isBusService(String s) {
    return s.matches("^(\\d{1,3}[ABCEGMNRWXe]?)|(NR\\d)|(CT8)|(CT18)");
  }

  public static boolean isBusStop(String s) {
    return s.matches("^\\d{5}");
  }

  public static boolean isLRT(String s) {
    return s.matches(
        "(^BP\\d{1,2}$)|(^STC$)|(^PTC$)|(^SE\\d{1,2}$)|(^SW\\d{1,2}$)|(^PE\\d{1,2}$)|(^PW\\d{1,2}$)");
  }

  public static boolean isInService(ZonedDateTime dt, String s) {
    if (Utils.isLRT(s)) return dt.getHour() >= 5 || dt.getHour() <= 1;
    if (dt.getHour() == 0) return dt.getMinute() <= 30;
    if (dt.getHour() >= 5) return dt.getHour() > 5 || dt.getMinute() >= 30;
    return false;
  }

  public static double getFreq(ZonedDateTime dt, double[] freq) {
    if (dt.getHour() >= 19 || dt.getHour() < 6 || (dt.getHour() == 6 && dt.getHour() < 30))
      return freq[1];
    if (dt.getHour() >= 17) return freq[3];
    if (dt.getHour() > 8 || (dt.getHour() == 8 && dt.getMinute() >= 30)) return freq[0];
    return freq[2];
  }

  // https://stackoverflow.com/questions/16266809/convert-from-latitude-longitude-to-x-y
  static final double cosLat = 0.2169571866380666972; // cos(lat of SG) = cos(1.3521)
  static final double r = 6378.1;

  public static Coordinate approxXY(double lat, double lng) {
    // return new Coordinate(r * lng * cosLat, r * lat);
    return new Coordinate(lng * cosLat, lat);
  }

  public static String getLatLonWKT() {
    return "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS"
               + " 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
  }
}
