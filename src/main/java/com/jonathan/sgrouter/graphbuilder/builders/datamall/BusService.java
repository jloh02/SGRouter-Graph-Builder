package com.jonathan.sgrouter.graphbuilder.builders.datamall;

public class BusService {
  double[] freq = new double[4]; // AM_off, PM_off, AM_peak, PM_peak

  public BusService(String[] freq) {
    for (int i = 0; i < 4; i++) {
      if (validFreq(freq[i])) this.freq[i] = processFreq(freq[i]);
      else this.freq[i] = -1;
    }
  }

  public double[] getFreqArr() {
    return freq;
  }

  boolean validFreq(String s) {
    return s.matches("(^\\d{1,2})|(^\\d{1,2}-\\d{1,2})");
  }

  double processFreq(String s) {
    String[] startEnd = s.split("-");
    if (startEnd.length == 1) return Double.parseDouble(startEnd[0]);
    return (Double.parseDouble(startEnd[0]) + Double.parseDouble(startEnd[1])) / 2;
  }

  @Override
  public String toString() {
    return String.format("%f %f %f %f", freq[0], freq[1], freq[2], freq[3]);
  }
}
