package com.jonathan.sgrouter.graphbuilder.config;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gmap")
public class GmapConfig {
  String localApiKey;

  public String getApiKey() {
    return GraphBuilderApplication.appengineDeployment
        ? DatastoreHandler.getValue("GMAP_API_KEY")
        : localApiKey;
  }
}
