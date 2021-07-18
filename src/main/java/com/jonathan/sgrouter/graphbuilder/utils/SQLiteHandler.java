package com.jonathan.sgrouter.graphbuilder.utils;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.models.Node;
import com.jonathan.sgrouter.graphbuilder.models.Vertex;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class SQLiteHandler {
  Connection conn;

  static {
    File dbFolder = new File("dbs");

    if (dbFolder.exists()) {
      for (String s : dbFolder.list()) {
        File f = new File(dbFolder.getPath(), s);
        if (!f.delete()) throw new RuntimeException("Unable to delete graph.db");
      }
    }

    dbFolder.mkdirs();
  }

  public SQLiteHandler(String filename) {
    filename = "dbs/" + filename;
    if (GraphBuilderApplication.appengineDeployment) filename = "/tmp/" + filename;
    try {
      this.conn = DataSource.getConnection(filename);
      Statement s = conn.createStatement();
      s.execute(
          "CREATE TABLE IF NOT EXISTS nodes(src TEXT PRIMARY KEY, name TEXT, lat NUMERIC, lon"
              + " NUMERIC)");
      s.execute(
          "CREATE TABLE IF NOT EXISTS vertex(src TEXT, des TEXT, service TEXT, time NUMERIC,"
              + " FOREIGN KEY(src) REFERENCES nodes(src), FOREIGN KEY(des) REFERENCES nodes(src),"
              + " PRIMARY KEY (src,des,service))");
      s.execute("CREATE TABLE IF NOT EXISTS freqs(service TEXT PRIMARY KEY, freq NUMERIC)");
      conn.setAutoCommit(false);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Unable to connect to database");
    }
  }

  public void addNodes(ArrayList<Node> nodeList) {
    try {
      PreparedStatement ps =
          conn.prepareStatement("INSERT INTO nodes(src,name,lat,lon) VALUES(?,?,?,?)");
      for (Node n : nodeList) {
        ps.setString(1, n.getSrcKey());
        ps.setString(2, n.getName());
        ps.setDouble(3, n.getLat());
        ps.setDouble(4, n.getLon());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      log.error(e.getMessage());
    }
  }

  public void addFreq(HashMap<String, Double> freqs) {
    try {
      PreparedStatement ps = conn.prepareStatement("INSERT INTO freqs(service,freq) VALUES(?,?)");
      for (Entry<String, Double> e : freqs.entrySet()) {
        ps.setString(1, e.getKey());
        ps.setDouble(2, e.getValue());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      log.error(e.getMessage());
    }
  }

  public void addVertices(ArrayList<Vertex> vtxList) {
    try {
      // ON CONFLICT: Use lower time (e.g. src=80199 service=11)
      PreparedStatement ps =
          conn.prepareStatement(
              "INSERT INTO vertex(src,des,service,time) VALUES(?,?,?,?) ON CONFLICT(src, des,"
                  + " service) DO UPDATE SET time=excluded.time WHERE excluded.time<vertex.time");
      for (Vertex v : vtxList) {
        ps.setString(1, v.getSrc());
        ps.setString(2, v.getDes());
        ps.setString(3, v.getService());
        ps.setDouble(4, v.getTime());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      log.error(e.getMessage());
    }
  }

  public void commit() {
    try {
      conn.commit();
      conn.close();
    } catch (SQLException e) {
      log.error(e.getMessage());
    }
  }
}
