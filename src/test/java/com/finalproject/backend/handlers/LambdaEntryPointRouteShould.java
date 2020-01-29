package com.finalproject.backend.handlers;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.common.BaseRouteTest;
import org.apache.camel.Exchange;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LambdaEntryPointRouteShould extends BaseRouteTest {

  @Before
  public void setUp() throws IOException {
    when(amazonS3.getObject(anyString(), anyString())).thenReturn(new S3Object());
    S3EventNotification s3EventNotification = S3Event.parseJson(readFileToString(new File("src/test/resources/event.json"), UTF_8));
    s3Event = new S3Event(s3EventNotification.getRecords());
  }

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