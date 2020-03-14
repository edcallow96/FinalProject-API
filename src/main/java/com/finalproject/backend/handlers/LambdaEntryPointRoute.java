package com.finalproject.backend.handlers;

import com.finalproject.backend.archive.ZipFilePredicate;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.apache.camel.support.builder.PredicateBuilder.and;
import static org.apache.camel.support.builder.PredicateBuilder.not;

@Component
public class LambdaEntryPointRoute extends RouteBuilder {

  private final PrepareJobProcessor prepareJobProcessor;
  private final JobFailedPredicate jobFailedPredicate;
  private final ZipFilePredicate isZipFile;

  public LambdaEntryPointRoute(
      final PrepareJobProcessor prepareJobProcessor,
      final JobFailedPredicate jobFailedPredicate,
      final ZipFilePredicate isZipFile) {
    this.prepareJobProcessor = prepareJobProcessor;
    this.jobFailedPredicate = jobFailedPredicate;
    this.isZipFile = isZipFile;
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
            .to(UNZIP_FILE_ROUTE)
          .otherwise()
            .to(PROCESS_JOB_ROUTE)
        .end();

    from(PROCESS_JOB_ROUTE)
        .filter(and(not(jobFailedPredicate), simple("${body.contentType} == null")))
          .to(FILE_IDENTIFICATION_ROUTE)
        .end()
        .filter(not(jobFailedPredicate))
          .to(THREAT_REMOVAL_ROUTE)
        .end()
        .filter(not(jobFailedPredicate))
          .to(ANTI_VIRUS_SCANNING_ROUTE)
        .end();

    from(JOB_COMPLETION_ROUTE)
        .log("Finished Job ${body.jobId}")
        .choice()
          .when(jobFailedPredicate)
            .to(SEND_FAILURE_NOTIFICATION_ROUTE)
          .otherwise()
            .to(SEND_SUCCESS_NOTIFICATION_ROUTE)
        .end();
    //@formatter:on
  }
}
