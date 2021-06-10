package com.jonathan.sgrouter.graphbuilder.models.config;

import java.util.ArrayList;
import java.util.HashMap;
import lombok.Data;

@Data
public class TrainConfig {
  public DefaultTiming mrt, lrt;
  HashMap<String, String> nameToIds;
  HashMap<String, TrainServiceName> services;
  String excludeLineBranch, excludeLineLoop;
  double transferTime;
  ArrayList<BranchConfig> branches;
  ArrayList<String> invalidStations;
  ArrayList<String[]> loops;
  double[] freq;

  public String getExcludeLine() {
    return String.format("%s|%s", excludeLineBranch, excludeLineLoop);
  }
}
