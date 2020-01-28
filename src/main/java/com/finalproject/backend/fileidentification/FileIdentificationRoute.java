package com.finalproject.backend.fileidentification;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.FILE_IDENTIFICATION_ROUTE;

@Component
public class FileIdentificationRoute extends RouteBuilder {
  @Override
  public void configure() throws Exception {
    //formatter:off
    from(FILE_IDENTIFICATION_ROUTE)
        .log("File identification")
        .end();
    //formatter:on
  }
}
