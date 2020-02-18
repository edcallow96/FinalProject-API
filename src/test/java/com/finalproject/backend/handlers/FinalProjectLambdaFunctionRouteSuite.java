package com.finalproject.backend.handlers;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.common.BaseRouteTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

public class FinalProjectLambdaFunctionRouteSuite extends BaseRouteTest {

  @Autowired
  protected FinalProjectLambdaFunction finalProjectLambdaFunction;

  @Before
  public void setUp() throws IOException {
    super.setUp();
    S3EventNotification s3EventNotification = S3Event.parseJson(readFileToString(new File("src/test/resources/event.json"), UTF_8));
    s3Event = new S3Event(s3EventNotification.getRecords());
    S3EventNotification.S3ObjectEntity s3ObjectEntity = s3EventNotification.getRecords().get(0).getS3().getObject();
    S3Object s3Object = new S3Object();
    s3Object.setKey(s3ObjectEntity.getKey());
    s3Object.setObjectContent(new ByteArrayInputStream(new byte[]{}));
    when(amazonS3.getObject(anyString(), anyString())).thenReturn(s3Object);
  }

  @Test
  public void exchangeIsSentToEntryPointRoute() throws Exception {
    mockEntryPointEndpoint.setExpectedCount(1);
    mockSendSuccessNotificationEndpoint.setExpectedCount(1);

    finalProjectLambdaFunction.apply(s3Event);

    mockEntryPointEndpoint.assertIsSatisfied();
    mockSendSuccessNotificationEndpoint.assertIsSatisfied();
  }

}