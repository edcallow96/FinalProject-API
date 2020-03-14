package com.finalproject.backend.archive;

import com.finalproject.backend.common.BaseRouteTest;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@MockEndpoints(UNZIP_FILE_ROUTE + "|" + ZIP_FILE_ROUTE)
public class HandleArchiveRouteSuite extends BaseRouteTest {

  @EndpointInject("mock:" + ZIP_FILE_ROUTE)
  private MockEndpoint mockZipFileEndpoint;

  @EndpointInject("mock:" + PROCESS_JOB_ROUTE)
  private MockEndpoint mockProcessJobEndpoint;

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

    mockProcessJobEndpoint.setExpectedCount(10);
    templateProducer.send(UNZIP_FILE_ROUTE, exchange);

    verify(unZipProcessor).process(exchange);
    mockProcessJobEndpoint.assertIsSatisfied();
  }

  @Test
  public void zipFieRouteShouldAggregateExtractedFileJobs() throws Exception {
    unzipFileRouteShouldSendEachExtractedFileToProcessJobRoute();

    verify(zipProcessor).process(any(Exchange.class));
  }

}