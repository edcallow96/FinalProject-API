package com.finalproject.backend.threatremoval;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.THREAT_REMOVAL_ROUTE;
import static com.finalproject.backend.model.ProcessName.THREAT_REMOVAL;

@Component
public class ThreatRemovalRoute extends RouteBuilder {

  private final ThreatRemovalProcessor threatRemovalProcessor;
  private final SupportedThreatRemovalTypePredicate supportedThreatRemovalType;

  public ThreatRemovalRoute(final ThreatRemovalProcessor threatRemovalProcessor,
                            final SupportedThreatRemovalTypePredicate supportedThreatRemovalType) {
    this.threatRemovalProcessor = threatRemovalProcessor;
    this.supportedThreatRemovalType = supportedThreatRemovalType;
  }

  @Override
  public void configure() {
    //@formatter:off
    from(THREAT_REMOVAL_ROUTE)
        .routeId(THREAT_REMOVAL.name())
        .log("Threat removal")
        .filter(supportedThreatRemovalType)
          .process(threatRemovalProcessor)
        .end();
    //@formatter:on
  }
}
