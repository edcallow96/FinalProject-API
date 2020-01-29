package com.finalproject.backend.fileidentification;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.FILE_IDENTIFICATION_ROUTE;
import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_FAILURE_NOTIFICATION;
import static org.apache.camel.LoggingLevel.ERROR;

@Component
public class FileIdentificationRoute extends RouteBuilder {

  private final FileIdentificationProcessor fileIdentificationProcessor;

  public FileIdentificationRoute(final FileIdentificationProcessor fileIdentificationProcessor) {
    this.fileIdentificationProcessor = fileIdentificationProcessor;
  }

  @Override
  public void configure() throws Exception {
    //formatter:off
    onException(Exception.class)
        .log(ERROR, "Exception occurred during file identification. ${exception.message}")
        .handled(true)
        .maximumRedeliveries(3)
        .redeliveryDelay(0)
        .log("Maximum redelivery attempted during file identification, failing job")
        .to(SEND_FAILURE_NOTIFICATION);

    from(FILE_IDENTIFICATION_ROUTE)
        .process(fileIdentificationProcessor)
        .end();
    //formatter:on
  }
}
