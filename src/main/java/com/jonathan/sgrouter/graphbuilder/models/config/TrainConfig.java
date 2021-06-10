package com.jonathan.sgrouter.graphbuilder.models.config;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TrainConfig {
  public DefaultTiming mrt, lrt;
  Map<String, String> nameToIds;
  Map<String, TrainServiceName> services;
  String excludeLineBranch, excludeLineLoop;
  double transferTime;
  List<BranchConfig> branches;
  List<String> invalidStations;
  List<String[]> loops;
  double[] freq;

  public String getExcludeLine() {
    return String.format("%s|%s", excludeLineBranch, excludeLineLoop);
  }
}
