package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import com.jonathan.sgrouter.graphbuilder.utils.DatastoreHandler;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Slf4j
public class CronFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;

    String appengineHeader = req.getHeader("X-Appengine-Country");

    GraphBuilderApplication.appengineDeployment = appengineHeader != null;

    if (!GraphBuilderApplication.appengineDeployment) AnsiOutput.setEnabled(Enabled.ALWAYS);

    log.debug("App Engine Deployment: {}", GraphBuilderApplication.appengineDeployment);

    log.debug(Collections.list(req.getHeaderNames()).toString());
    if (GraphBuilderApplication.appengineDeployment) {
      String cronHeader = req.getHeader("X-Appengine-Cron");
      if (cronHeader == null || !cronHeader.equals("true")) {
        log.error("Invalid cron request: {}", req);
        ((HttpServletResponse) response)
            .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized to call this API");
        return;
      }

      String retryHead = req.getHeader("X-Appengine-TaskRetryCount");
      int retry = Integer.parseInt(retryHead);
      log.debug("{} {}", retryHead, retry);
      if (retry == 0) DatastoreHandler.setProgress(0);

      String reason = req.getHeader("X-Appengine-TaskRetryReason");
      if (reason != null) {
        log.debug(reason);
        log.debug(req.getHeader("X-Appengine-TaskPreviousResponse"));
      }
    }
    chain.doFilter(request, response);

    System.exit(0);
  }
}
