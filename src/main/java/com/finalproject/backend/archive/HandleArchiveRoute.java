package com.finalproject.backend.archive;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.PROCESS_JOB;

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

    from("direct:unzipFileRoute")
        .process(unZipProcessor)
        .split(simple("${body}"))
          .to(PROCESS_JOB)
        .end()
        .to("direct:zipFileRoute");

    from("direct:zipFileRoute")
        .aggregate(constant(true), new GroupedMessageAggregationStrategy())
          .completionTimeout(1000)
            .log(LoggingLevel.ERROR, "Aggregation failed to complete in time")
        .end()
        .log("Aggregated body: ${body}")
        .process(zipProcessor);

    //@formatter:on
  }
}
