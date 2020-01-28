package com.finalproject.backend.threatremoval;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.THREAT_REMOVAL_ROUTE;

@Component
public class ThreatRemovalRoute extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from(THREAT_REMOVAL_ROUTE)
        .log("Threat removal")
        .end();
  }
}
