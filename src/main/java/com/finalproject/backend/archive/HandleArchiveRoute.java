package com.finalproject.backend.archive;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.apache.camel.Exchange.AGGREGATED_SIZE;
import static org.apache.camel.Exchange.SPLIT_SIZE;

@Component
public class HandleArchiveRoute extends RouteBuilder {

  private final ZipProcessor zipProcessor;
  private final UnZipProcessor unZipProcessor;
  private final ProcessJobAggregationStrategy processJobAggregationStrategy;

  public HandleArchiveRoute(ZipProcessor zipProcessor,
                            UnZipProcessor unZipProcessor,
                            ProcessJobAggregationStrategy processJobAggregationStrategy) {
    this.zipProcessor = zipProcessor;
    this.unZipProcessor = unZipProcessor;
    this.processJobAggregationStrategy = processJobAggregationStrategy;
  }

  @Override
  public void configure() {
    //@formatter:off
    from(UNZIP_FILE_ROUTE)
        .process(unZipProcessor)
        .split(body())
          .to(PROCESS_JOB_ROUTE)
          .to(ZIP_FILE_ROUTE);

    from(ZIP_FILE_ROUTE)
        .aggregate(header(AMAZON_REQUEST_ID), processJobAggregationStrategy)
          .completionPredicate(exchangeProperty(AGGREGATED_SIZE).isGreaterThanOrEqualTo(exchangeProperty(SPLIT_SIZE)))
          .closeCorrelationKeyOnCompletion(0)
          .log("Aggregated body: ${body}")
          .process(zipProcessor);
    //@formatter:on
  }
}
