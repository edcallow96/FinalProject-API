package com.finalproject.backend.handlers;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.apache.camel.support.builder.PredicateBuilder.and;
import static org.apache.camel.support.builder.PredicateBuilder.not;
import static org.apache.tika.mime.MediaType.APPLICATION_ZIP;

@Component
public class LambdaEntryPointRoute extends RouteBuilder {

  private final PrepareJobProcessor prepareJobProcessor;
  private final JobFailedPredicate jobFailedPredicate;
  private final Predicate isZipFile = exchange -> exchange.getIn().getBody(ProcessJob.class).getContentType() != null
      && exchange.getIn().getBody(ProcessJob.class).getContentType().equals(APPLICATION_ZIP);

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
        .log(LoggingLevel.ERROR, "${exception.stacktrace}")
        .handled(true);

    from(ENTRY_POINT_ROUTE)
        .process(prepareJobProcessor)
        .to(FILE_IDENTIFICATION_ROUTE)
        .choice()
          .when(isZipFile)
            .to("direct:unzipFileRoute")
          .otherwise()
            .to(PROCESS_JOB)
        .end()
        .log("Finished ${body}")
        .choice()
          .when(jobFailedPredicate)
            .to(SEND_FAILURE_NOTIFICATION)
          .otherwise()
            .to(SEND_SUCCESS_NOTIFICATION)
        .end();

    from(PROCESS_JOB)
        .filter(and(not(jobFailedPredicate), simple("${body.contentType} == null")))
          .to(FILE_IDENTIFICATION_ROUTE)
        .end()
        .filter(not(jobFailedPredicate))
          .to(THREAT_REMOVAL_ROUTE)
        .end()
        .filter(not(jobFailedPredicate))
          .to(ANTI_VIRUS_SCANNING_ROUTE)
        .end();
    //@formatter:on
  }
}
