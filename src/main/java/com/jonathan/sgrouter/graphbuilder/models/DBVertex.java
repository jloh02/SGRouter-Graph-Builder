package com.jonathan.sgrouter.graphbuilder.models;

import lombok.Data;

@Data
public class DBVertex {
	String src, des, service;
	double time;

	public DBVertex(){}

	public DBVertex(String src, String des, String service, double time) {
		this.src = src;
		this.des = des;
		this.service = service;
		this.time = time;
	}
}
