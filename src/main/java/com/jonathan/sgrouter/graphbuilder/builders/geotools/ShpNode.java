package com.jonathan.sgrouter.graphbuilder.builders.geotools;

import lombok.Data;

@Data
public class ShpNode implements Comparable<ShpNode>{
	String id,name;
	double lat,lon;

	public ShpNode(ShpNode n){
		this.id = n.getId();
		this.name = n.getName();
		this.lat = n.getLat();
		this.lon = n.getLon();
	}

	public ShpNode(String id, String name, double lat, double lon){
		this.id=id;
		this.name=name;
		this.lat=lat;
		this.lon=lon;
	}

	@Override
	public int compareTo(ShpNode o) {
		String idA = this.id;
		String idB = o.id;
		if(idA.substring(0,2).equals(idB.substring(0,2))) return Integer.parseInt(idA.substring(2))-Integer.parseInt(idB.substring(2));
		return idA.compareTo(idB);
	}

	// @Override
	// public String toString(){
	// 	return String.format("%s (%f,%f): %s",id,lat,lon,name);
	// }
}
