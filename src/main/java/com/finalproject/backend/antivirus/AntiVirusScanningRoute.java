package com.finalproject.backend.antivirus;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.ANTI_VIRUS_SCANNING_ROUTE;
import static com.finalproject.backend.model.ProcessName.ANTI_VIRUS_SCAN;

@Component
public class AntiVirusScanningRoute extends RouteBuilder {

  private final AntiVirusProcessor antiVirusProcessor;

  public AntiVirusScanningRoute(final AntiVirusProcessor antiVirusProcessor) {
    this.antiVirusProcessor = antiVirusProcessor;
  }

  @Override
  public void configure() throws Exception {
    from(ANTI_VIRUS_SCANNING_ROUTE)
        .log("AV scanning")
        .routeId(ANTI_VIRUS_SCAN.name())
        .process(antiVirusProcessor)
        .end();
  }
}
