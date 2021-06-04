package com.jonathan.sgrouter.graphbuilder.models;

import java.util.List;

import com.jonathan.sgrouter.graphbuilder.models.config.TrainServiceName;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BranchInfo {
	public List<Double> cumDist;
	public List<String> nodes;
	double transferTime;
	boolean join; //Whether branch is joining or splitting
	TrainServiceName postBranchService,branchService;
}
