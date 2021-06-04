package com.jonathan.sgrouter.graphbuilder.builders.datamall;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Map;

import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class DatamallBus<T1, T2> {
	Map<T1, T2> output;

	abstract String initDatamallType();
	abstract void processData(JSONArray value);

	private JSONObject connect(String urlPath) {
		String urlString = "http://datamall2.mytransport.sg/ltaodataservice/" + urlPath;
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest req = HttpRequest.newBuilder(URI.create(urlString)).header("AccountKey", DatastoreHandler.getValue("DATAMALL_API_KEY")).build();
			HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
			return new JSONObject(res.body().toString());
		} catch (Exception e) {
			System.out.println(e);
		}
		return new JSONObject();
	}

	public Map<T1, T2> getBusData() {
		String datamallType = initDatamallType();
		output = new HashMap<T1, T2>();
		JSONObject json;
		JSONArray arr;
		while (true) {
			json = connect(datamallType + "?$skip=" + output.size());
			try {
				arr = json.getJSONArray("value");
				if (arr.length() == 0)
					break; // End condition
				processData(arr);
			} catch (JSONException e) {
				System.out.println(e);
			}
		}
		return output;
	}
}