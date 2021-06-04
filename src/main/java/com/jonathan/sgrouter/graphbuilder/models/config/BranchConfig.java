package com.jonathan.sgrouter.graphbuilder.models.config;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class BranchConfig{
	String branchNode, src, des;
	boolean join;
	double transferTime;
	TrainServiceName postBranchService,branchService;
}
