package com.jonathan.sgrouter.graphbuilder.utils;

import java.util.HashMap;
import java.util.Map;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class DatastoreHandler {
	final static Map<String,String> values = new HashMap<>(); 
	final static Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	final static String kind = "keys";

	public static String getValue(String name) {
		if(values.containsKey(name)) return values.get(name);
		return connect(name);
	}

	static String connect(String name){
		try {
			Key taskKey = datastore.newKeyFactory().setNamespace("sgrouter").setKind(kind).newKey(name);
			Entity retrieved = datastore.get(taskKey);
			values.put(name,retrieved.getString("value"));
			return values.get(name);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
		return "";
	}
}
