package com.finalproject.backend.threatremoval;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.THREAT_REMOVAL_ROUTE;
import static com.finalproject.backend.model.ProcessName.THREAT_REMOVAL;

@Component
public class ThreatRemovalRoute extends RouteBuilder {

  private final ThreatRemovalProcessor threatRemovalProcessor;

  public ThreatRemovalRoute(final ThreatRemovalProcessor threatRemovalProcessor) {
    this.threatRemovalProcessor = threatRemovalProcessor;
  }

  @Override
  public void configure() {
    from(THREAT_REMOVAL_ROUTE)
        .log("Threat removal")
        .routeId(THREAT_REMOVAL.name())
        .process(threatRemovalProcessor)
        .end();
  }
}
