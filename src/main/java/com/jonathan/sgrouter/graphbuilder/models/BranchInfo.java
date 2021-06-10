package com.jonathan.sgrouter.graphbuilder.models;

import com.jonathan.sgrouter.graphbuilder.models.config.TrainServiceName;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BranchInfo {
  public ArrayList<Double> cumDist;
  public ArrayList<String> nodes;
  double transferTime;
  boolean join; // Whether branch is joining or splitting
  TrainServiceName postBranchService, branchService;
}
