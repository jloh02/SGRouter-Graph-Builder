package com.jonathan.sgrouter.graphbuilder.config;

import com.jonathan.sgrouter.graphbuilder.models.config.DefaultTiming;
import com.jonathan.sgrouter.graphbuilder.models.config.TrainConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("graphbuilder")
public class GraphBuilderConfig {
	public DefaultTiming bus;
	public TrainConfig train;
	double defaultWalkingSpeed;
	double maximumBusTrainDist;
}

