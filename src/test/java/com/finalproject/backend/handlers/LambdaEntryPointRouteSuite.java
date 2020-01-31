package com.finalproject.backend.handlers;

import com.finalproject.backend.common.BaseRouteTest;
import org.apache.camel.Exchange;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LambdaEntryPointRouteSuite extends BaseRouteTest {

  @Test
  public void sendExchangeToEntryPointRoute() throws Exception {
    mockEntryPointEndpoint.setExpectedCount(1);
    mockSendSuccessNotificationEndpoint.setExpectedCount(1);

    finalProjectLambdaFunction.apply(s3Event);

    mockEntryPointEndpoint.assertIsSatisfied();
    mockSendSuccessNotificationEndpoint.assertIsSatisfied();
  }

  @Test
  public void retryFailuresDuringFileIdentification() throws Exception {
    doThrow(new RuntimeException()).when(fileIdentificationProcessor).process(any(Exchange.class));
    mockFileIdentificationEndpoint.setExpectedCount(1);

    finalProjectLambdaFunction.apply(s3Event);

    mockFileIdentificationEndpoint.assertIsSatisfied();
    verify(fileIdentificationProcessor, times(4)).process(any(Exchange.class));
  }
}