package com.finalproject.backend.antivirus;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.ANTI_VIRUS_SCANNING_ROUTE;

@Component
public class AntiVirusScanningRoute extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    from(ANTI_VIRUS_SCANNING_ROUTE)
        .log("AV scanning")
        .end();
  }
}
