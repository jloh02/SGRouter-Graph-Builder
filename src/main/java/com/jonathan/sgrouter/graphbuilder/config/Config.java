package com.jonathan.sgrouter.graphbuilder.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties
public class Config {
	public boolean appengineDeployment;
	@Autowired
	public GmapConfig gmap;
	@Autowired
	public GraphBuilderConfig graphbuilder;

	public Config() {
	}

	public Config(Config config) {
		this.appengineDeployment=config.appengineDeployment;
		this.gmap = config.gmap;
		this.graphbuilder = config.graphbuilder;
	}
}
