package com.jonathan.sgrouter.graphbuilder.models;

import lombok.Data;

@Data
public class Vertex {
	String src, des, service;
	double time;

	public Vertex(){}

	public Vertex(String src, String des, String service, double time) {
		this.src = src;
		this.des = des;
		this.service = service;
		this.time = time;
	}
}
