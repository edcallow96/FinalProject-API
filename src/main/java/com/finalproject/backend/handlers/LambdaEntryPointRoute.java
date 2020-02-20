package com.finalproject.backend.handlers;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.apache.camel.support.builder.PredicateBuilder.not;

@Component
public class LambdaEntryPointRoute extends RouteBuilder {

  private final PrepareJobProcessor prepareJobProcessor;
  private final JobFailedPredicate jobFailedPredicate;

  public LambdaEntryPointRoute(
      final PrepareJobProcessor prepareJobProcessor,
      final JobFailedPredicate jobFailedPredicate) {
    this.prepareJobProcessor = prepareJobProcessor;
    this.jobFailedPredicate = jobFailedPredicate;
  }

  @Override
  public void configure() {
    //@formatter:off

    onException(Exception.class)
        .log(LoggingLevel.ERROR, "Exception received ${exception.message}" )
        .log(LoggingLevel.DEBUG, "${exception.stacktrace}")
        .handled(true);

    from(ENTRY_POINT_ROUTE)
        .process(prepareJobProcessor)
        .to(PROCESS_JOB)
        .choice()
          .when(jobFailedPredicate)
            .to(SEND_FAILURE_NOTIFICATION)
          .otherwise()
            .to(SEND_SUCCESS_NOTIFICATION)
        .end();

    from(PROCESS_JOB)
        .to(FILE_IDENTIFICATION_ROUTE)
        .filter(not(jobFailedPredicate))
          .to(THREAT_REMOVAL_ROUTE)
        .end()
        .filter(not(jobFailedPredicate))
          .to(ANTI_VIRUS_SCANNING_ROUTE)
        .end();
    //@formatter:on
  }
}
