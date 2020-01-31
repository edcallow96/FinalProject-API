package com.finalproject.backend.handlers;

import com.finalproject.backend.antivirus.VirusDetectedPredicate;
import com.finalproject.backend.threatremoval.SupportedThreatRemovalTypePredicate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;

@Component
public class LambdaEntryPointRoute extends RouteBuilder {

  private final SupportedThreatRemovalTypePredicate supportedThreatRemovalType;
  private final VirusDetectedPredicate virusDetected;

  public LambdaEntryPointRoute(
      final SupportedThreatRemovalTypePredicate supportedThreatRemovalType,
      final VirusDetectedPredicate virusDetected) {
    this.supportedThreatRemovalType = supportedThreatRemovalType;
    this.virusDetected = virusDetected;
  }

  @Override
  public void configure() {
    //@formatter:off

    onException(Exception.class)
        .log(LoggingLevel.ERROR, "Exception received ${exception.stacktrace}" )
        .maximumRedeliveries(1)
        .redeliveryDelay(0);

    from(ENTRY_POINT_ROUTE)
        .log(LoggingLevel.INFO, "Received exchange. Uploaded file size: ${body.length}")
        .to(FILE_IDENTIFICATION_ROUTE)
        .filter(supportedThreatRemovalType)
          .to(THREAT_REMOVAL_ROUTE)
        .end()
        .to(ANTI_VIRUS_SCANNING_ROUTE)
        .choice()
          .when(virusDetected)
            .to(SEND_FAILURE_NOTIFICATION)
            .otherwise()
              .to(SEND_SUCCESS_NOTIFICATION)
        .end();
    //@formatter:on
  }
}
