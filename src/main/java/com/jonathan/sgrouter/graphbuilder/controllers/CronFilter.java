package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import java.io.IOException;
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

    if (GraphBuilderApplication.appengineDeployment) {
      String cronHeader = req.getHeader("X-Appengine-Cron");
      if (cronHeader == null || !cronHeader.equals("true")) {
        log.warn("Invalid cron request: {}", req);
        ((HttpServletResponse) response)
            .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized to call this API");
        return;
      }
    }
    chain.doFilter(request, response);
    if (req.getParameter("hHigh") != null && req.getParameter("hHigh").equals("24")) System.exit(0);
  }
}
