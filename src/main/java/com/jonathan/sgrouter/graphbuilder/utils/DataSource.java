package com.jonathan.sgrouter.graphbuilder.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Static class to local in-memory database connection */
public class DataSource {
  private static HikariDataSource ds;
  private static HikariConfig config;

  /** Retrieves a @link{java.sql.Connection} to local in-memory database */
  public static Connection getConnection(String filename) throws SQLException {
    config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + filename);
    config.setMaximumPoolSize(3);
    config.setMinimumIdle(2);
    ds = new HikariDataSource(config);
    return ds.getConnection();
  }

  /** Closes the entire datasource */
  public static void close() {
    ds.close();
  }
}
