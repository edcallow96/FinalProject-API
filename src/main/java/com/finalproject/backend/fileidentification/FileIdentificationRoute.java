package com.finalproject.backend.fileidentification;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.FILE_IDENTIFICATION_ROUTE;
import static com.finalproject.backend.model.ProcessName.FILE_IDENTIFICATION;
import static org.apache.camel.LoggingLevel.ERROR;

@Component
public class FileIdentificationRoute extends RouteBuilder {

  private final FileIdentificationProcessor fileIdentificationProcessor;

  public FileIdentificationRoute(final FileIdentificationProcessor fileIdentificationProcessor) {
    this.fileIdentificationProcessor = fileIdentificationProcessor;
  }

  @Override
  public void configure() {
    //formatter:off
    onException(Exception.class)
        .log(ERROR, "Exception occurred during file identification. ${exception.message}")
        .handled(true)
        .end();

    from(FILE_IDENTIFICATION_ROUTE)
        .routeId(FILE_IDENTIFICATION.name())
        .process(fileIdentificationProcessor)
        .end();
    //formatter:on
  }
}
