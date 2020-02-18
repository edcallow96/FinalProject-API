package com.finalproject.backend.handlers;

import com.finalproject.backend.common.BaseRouteTest;
import org.junit.Test;

import static com.finalproject.backend.constants.BackendApplicationConstants.ENTRY_POINT_ROUTE;
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

}