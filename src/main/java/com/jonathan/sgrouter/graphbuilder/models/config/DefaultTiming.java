package com.jonathan.sgrouter.graphbuilder.models.config;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class DefaultTiming{
	double defaultSpeed,defaultStopTime,speedFactor;
}
