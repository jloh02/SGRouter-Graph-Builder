package com.jonathan.sgrouter.graphbuilder.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.List;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.models.DBVertex;
import com.jonathan.sgrouter.graphbuilder.models.Node;

import java.io.File;
import java.io.IOException;

public class SQLiteHandler {
	Connection conn;

	public SQLiteHandler() {
		String filename = GraphBuilderApplication.config.isAppengineDeployment() ? "/tmp/graph.db" : "graph.db";
		String dbUrl = "jdbc:sqlite:"+filename;
		try {
			File oldDbFile = new File(filename);
			if (oldDbFile.exists() && !oldDbFile.delete())
				throw new IOException("Unable to delete graph.db");

			this.conn = DriverManager.getConnection(dbUrl);
			Statement s = conn.createStatement();
			s.execute("CREATE TABLE IF NOT EXISTS nodes(src TEXT PRIMARY KEY, name TEXT, lat NUMERIC, lon NUMERIC)");
			s.execute(
					"CREATE TABLE IF NOT EXISTS vertex(src TEXT, des TEXT, service TEXT, time NUMERIC, FOREIGN KEY(src) REFERENCES nodes(src), FOREIGN KEY(des) REFERENCES nodes(src), PRIMARY KEY (src,des,service))");
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			System.err.println(e);
		} catch (IOException e){
			System.err.println(e);
			System.exit(1);
		}
	}

	public void addNodes(List<Node> nodeList) {
		try {
			PreparedStatement ps = conn.prepareStatement("INSERT INTO nodes(src,name,lat,lon) VALUES(?,?,?,?)");
			for (Node n : nodeList) {
				//System.out.println(n);
				ps.setString(1, n.getSrcKey());
				ps.setString(2, n.getName());
				ps.setDouble(3, n.getLat());
				ps.setDouble(4, n.getLon());
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	public void addVertices(List<DBVertex> vtxList) {
		try {
			// ON CONFLICT: Use lower time (e.g. src=80199 service=11)
			PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO vertex(src,des,service,time) VALUES(?,?,?,?) ON CONFLICT(src, des, service) DO UPDATE SET time=excluded.time WHERE excluded.time<vertex.time");
			for (DBVertex v : vtxList) {
				ps.setString(1, v.getSrc());
				ps.setString(2, v.getDes());
				ps.setString(3, v.getService());
				ps.setDouble(4, v.getTime());
				ps.executeUpdate();
			}			
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	public void commit(){
		commit(5);
	}

	void commit(int attempts){
		if(attempts == 0) return;
		try{
			conn.commit();
			conn.close();
		}catch(SQLException e){
			System.err.println(e);
			commit(4);
		}
	}

}
