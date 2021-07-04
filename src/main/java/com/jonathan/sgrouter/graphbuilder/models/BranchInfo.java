package com.jonathan.sgrouter.graphbuilder.models;

import com.jonathan.sgrouter.graphbuilder.models.config.TrainServiceName;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BranchInfo {
  boolean join; // Whether branch is joining or splitting
  TrainServiceName postBranchService, branchService;
}
