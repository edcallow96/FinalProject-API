package com.finalproject.backend.archive;

import com.finalproject.backend.common.BaseRouteTest;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MockEndpoints(UNZIP_FILE_ROUTE + "|" + ZIP_FILE_ROUTE)
public class HandleArchiveRouteSuite extends BaseRouteTest {

  @EndpointInject("mock:" + ZIP_FILE_ROUTE)
  private MockEndpoint mockZipFileEndpoint;

  @EndpointInject("mock:" + PROCESS_JOB_PIPELINE_ROUTE)
  private MockEndpoint mockProcessJobPipelineEndpoint;

  @EndpointInject("mock:" + JOB_COMPLETION_ROUTE)
  private MockEndpoint mockJobCompletionEndpoint;

  @MockBean
  private UnZipProcessor unZipProcessor;

  @MockBean
  private ZipProcessor zipProcessor;

  @Test
  public void unzipFileRouteShouldSendEachExtractedFileToProcessJobRoute() throws InterruptedException {
    List<ProcessJob> extractedFiles = new ArrayList<>();
    IntStream.range(0, 10).forEach(i -> extractedFiles.add(ProcessJob.builder().build()));
    doAnswer(invocationOnMock -> {
      exchange.getIn().setBody(extractedFiles);
      return null;
    }).when(unZipProcessor).process(exchange);

    mockProcessJobPipelineEndpoint.setExpectedCount(10);
    mockZipFileEndpoint.setExpectedCount(10);
    templateProducer.send(UNZIP_FILE_ROUTE, exchange);

    verify(unZipProcessor).process(exchange);
    mockProcessJobPipelineEndpoint.assertIsSatisfied();
    mockZipFileEndpoint.assertIsSatisfied();
  }

  @Test
  public void zipFieRouteShouldAggregateExtractedFileJobs() throws Exception {
    String jobId = randomAlphabetic(10);
    mockJobCompletionEndpoint.setExpectedCount(1);
    IntStream.range(0, 10).forEach(i -> {
      Exchange exchangeToBeAggregated = ExchangeBuilder.anExchange(camelContext).build();
      exchangeToBeAggregated.setProperty(Exchange.SPLIT_SIZE, 10);
      exchangeToBeAggregated.setProperty(Exchange.SPLIT_INDEX, i);
      exchangeToBeAggregated.getIn().setHeader(AMAZON_REQUEST_ID, jobId);
      exchangeToBeAggregated.getIn().setBody(ProcessJob.builder().build());
      templateProducer.send(ZIP_FILE_ROUTE, exchangeToBeAggregated);
    });

    verify(zipProcessor).process(any(Exchange.class));
    mockJobCompletionEndpoint.assertIsSatisfied();
  }

  @Test
  public void zipFieRouteAggregationCanHandleMultipleJobs() throws Exception {
    mockJobCompletionEndpoint.setExpectedCount(10);
    IntStream.range(0, 10).forEach(i -> {
      String jobId = randomAlphabetic(10);
      IntStream.range(0, 10).forEach(j -> {
        Exchange exchangeToBeAggregated = ExchangeBuilder.anExchange(camelContext).build();
        exchangeToBeAggregated.setProperty(Exchange.SPLIT_SIZE, 10);
        exchangeToBeAggregated.setProperty(Exchange.SPLIT_INDEX, j);
        exchangeToBeAggregated.getIn().setHeader(AMAZON_REQUEST_ID, jobId);
        exchangeToBeAggregated.getIn().setBody(ProcessJob.builder().build());
        templateProducer.send(ZIP_FILE_ROUTE, exchangeToBeAggregated);
      });
    });

    verify(zipProcessor, times(10)).process(any(Exchange.class));
    mockJobCompletionEndpoint.assertIsSatisfied();
  }

}