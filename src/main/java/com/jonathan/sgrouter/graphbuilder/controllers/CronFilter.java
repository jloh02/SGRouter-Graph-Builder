package com.jonathan.sgrouter.graphbuilder.controllers;

import com.jonathan.sgrouter.graphbuilder.GraphBuilderApplication;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Slf4j
public class CronFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (GraphBuilderApplication.config.appengineDeployment) {
      HttpServletRequest req = (HttpServletRequest) request;
      String cronHeader = req.getHeader("X-Appengine-Cron");
      if (cronHeader == null || !cronHeader.equals("true")) {
        log.warn("Invalid cron request: {}", req);
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
