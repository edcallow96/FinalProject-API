package com.finalproject.backend.common;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.fileidentification.FileIdentificationProcessor;
import com.finalproject.backend.handlers.FinalProjectLambdaFunction;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@MockEndpoints
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseRouteTest {

  @EndpointInject("mock:" + ENTRY_POINT_ROUTE)
  protected MockEndpoint mockEntryPointEndpoint;

  @EndpointInject("mock:" + FILE_IDENTIFICATION_ROUTE)
  protected MockEndpoint mockFileIdentificationEndpoint;

  @EndpointInject("mock:" + SEND_SUCCESS_NOTIFICATION)
  protected MockEndpoint mockSendSuccessNotificationEndpoint;

  @Autowired
  protected FinalProjectLambdaFunction finalProjectLambdaFunction;

  @Autowired
  protected ApplicationProperties applicationProperties;

  @MockBean
  protected AmazonS3 amazonS3;

  @MockBean
  protected FileIdentificationProcessor fileIdentificationProcessor;

  protected S3Event s3Event;

  @Before
  public void setUp() throws IOException {
    S3EventNotification s3EventNotification = S3Event.parseJson(readFileToString(new File("src/test/resources/event.json"), UTF_8));
    s3Event = new S3Event(s3EventNotification.getRecords());
    S3EventNotification.S3ObjectEntity s3ObjectEntity = s3EventNotification.getRecords().get(0).getS3().getObject();
    S3Object s3Object = new S3Object();
    s3Object.setKey(s3ObjectEntity.getKey());
    s3Object.setObjectContent(new ByteArrayInputStream(new byte[]{}));
    when(amazonS3.getObject(anyString(), anyString())).thenReturn(s3Object);
  }
}