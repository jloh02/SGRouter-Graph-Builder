package com.jonathan.sgrouter.graphbuilder.models.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class DefaultTiming {
  double defaultSpeed, defaultStopTime, speedFactor;
}
