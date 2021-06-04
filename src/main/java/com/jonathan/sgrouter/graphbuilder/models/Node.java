package com.jonathan.sgrouter.graphbuilder.models;

import java.util.List;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Node {
	String srcKey,name;
	double lat,lon;
	List<Vertex> edges;

	public Node(String key, String name, double lat, double lon){
		this.srcKey=key;
		this.name=name;
		this.lat=lat;
		this.lon=lon;
		edges = new ArrayList<>();
	}
}
