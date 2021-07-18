package com.jonathan.sgrouter.graphbuilder.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {
  private static HikariDataSource ds;
  private static HikariConfig config;

  public static Connection getConnection(String filename) throws SQLException {
    config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + filename);
    config.setMaximumPoolSize(3);
    config.setMinimumIdle(2);
    ds = new HikariDataSource(config);
    return ds.getConnection();
  }

  public static void close() {
    ds.close();
  }
}
