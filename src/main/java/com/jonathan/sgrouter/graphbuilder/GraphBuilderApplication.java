package com.jonathan.sgrouter.graphbuilder;

import com.jonathan.sgrouter.graphbuilder.config.Config;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class GraphBuilderApplication {
  public static boolean appengineDeployment;
  public static Config config;
  public static ZonedDateTime sgNow;

  @Autowired private Config cfgImport;

  public static void main(String[] args) {
    SpringApplication.run(GraphBuilderApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void doSomethingAfterStartup() {
    config = new Config(cfgImport);
  }
}
