package com.finalproject.backend.archive;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;

@Component
public class HandleArchiveRoute extends RouteBuilder {

  private final ZipProcessor zipProcessor;
  private final UnZipProcessor unZipProcessor;

  public HandleArchiveRoute(ZipProcessor zipProcessor,
                            UnZipProcessor unZipProcessor) {
    this.zipProcessor = zipProcessor;
    this.unZipProcessor = unZipProcessor;
  }

  @Override
  public void configure() {
    //@formatter:off

    from(UNZIP_FILE_ROUTE)
        .process(unZipProcessor)
        .split(simple("${body}"))
          .to(PROCESS_JOB_ROUTE)
        .end()
        .to(ZIP_FILE_ROUTE);

    from(ZIP_FILE_ROUTE)
        .aggregate(constant(true), new GroupedMessageAggregationStrategy())
          .completionTimeout(600000)
          .closeCorrelationKeyOnCompletion(0)
          .log(LoggingLevel.ERROR, "Aggregation failed to complete in time")
        .end()
        .log("Aggregated body: ${body}")
        .process(zipProcessor);

    //@formatter:on
  }
}
