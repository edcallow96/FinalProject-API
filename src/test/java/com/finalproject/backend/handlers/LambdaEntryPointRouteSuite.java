package com.finalproject.backend.handlers;

import com.finalproject.backend.common.BaseRouteTest;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import org.apache.tika.mime.MediaType;
import org.junit.Test;

import java.io.File;

import static com.finalproject.backend.constants.BackendApplicationConstants.ENTRY_POINT_ROUTE;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class LambdaEntryPointRouteSuite extends BaseRouteTest {

  @Test
  public void jobIsSentToBeProcessed() throws Exception {
    mockProcessJobEndpoint.setExpectedCount(1);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    mockProcessJobEndpoint.assertIsSatisfied();
    verify(prepareJobProcessor).process(exchange);
  }

  @Test
  public void jobIsSentForFileIdentification() throws InterruptedException {
    mockProcessJobEndpoint.setExpectedCount(1);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    mockFileIdentificationEndpoint.assertIsSatisfied();
    verify(fileIdentificationProcessor).process(exchange);
  }

  @Test
  public void jobSendsSuccessNotification() throws InterruptedException {
    mockSendSuccessNotificationEndpoint.setExpectedCount(1);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    mockSendSuccessNotificationEndpoint.assertIsSatisfied();
  }

  @Test
  public void jobSendsFailNotification() throws InterruptedException {
    mockSendFailureNotificationEndpoint.setExpectedCount(1);

    doAnswer((invocation) -> {
      exchange.getIn().getBody(ProcessJob.class).getProcessingResults().add(ProcessResult.builder().processStatus(FAILED).build());
      return null;
    }).when(fileIdentificationProcessor).process(exchange);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    mockSendFailureNotificationEndpoint.assertIsSatisfied();
  }

  @Test
  public void zipFileTest() throws Exception {
    doAnswer((invocation) -> {
      exchange.getIn().setBody(ProcessJob.builder()
          .payloadLocation(new File("src/test/resources/march.zip"))
          .sourceKey("march.zip")
          .contentType(MediaType.APPLICATION_ZIP)
          .build());
      return null;
    }).when(prepareJobProcessor).process(exchange);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);
  }

}