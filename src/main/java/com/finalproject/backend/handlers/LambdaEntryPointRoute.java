package com.finalproject.backend.handlers;

import com.finalproject.backend.antivirus.ThreadDetectedPredicate;
import com.finalproject.backend.threatremoval.SupportedThreatRemovalTypePredicate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;

@Component
public class LambdaEntryPointRoute extends RouteBuilder {

  private final SupportedThreatRemovalTypePredicate supportedThreatRemovalType;
  private final ThreadDetectedPredicate threatDetected;
  private final PrepareJobProcessor prepareJobProcessor;

  public LambdaEntryPointRoute(
      final SupportedThreatRemovalTypePredicate supportedThreatRemovalType,
      final ThreadDetectedPredicate threatDetected,
      final PrepareJobProcessor prepareJobProcessor) {
    this.supportedThreatRemovalType = supportedThreatRemovalType;
    this.threatDetected = threatDetected;
    this.prepareJobProcessor = prepareJobProcessor;
  }

  @Override
  public void configure() {
    //@formatter:off

    onException(Exception.class)
        .log(LoggingLevel.ERROR, "Exception received ${exception.message}" )
        .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
        .handled(true)
        .maximumRedeliveries(3)
        .redeliveryDelay(0)
        .log("Maximum redelivery attempted, failing job");

    from(ENTRY_POINT_ROUTE)
        .process(prepareJobProcessor)
        .to(FILE_IDENTIFICATION_ROUTE)
        .filter(supportedThreatRemovalType)
          .to(THREAT_REMOVAL_ROUTE)
        .end()
        .to(ANTI_VIRUS_SCANNING_ROUTE)
        .choice()
          .when(threatDetected)
            .to(SEND_FAILURE_NOTIFICATION)
            .otherwise()
              .to(SEND_SUCCESS_NOTIFICATION)
        .end();
    //@formatter:on
  }
}
