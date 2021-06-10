package com.jonathan.sgrouter.graphbuilder.models.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class BranchConfig {
  String branchNode, src, des;
  boolean join;
  double transferTime;
  TrainServiceName postBranchService, branchService;
}
