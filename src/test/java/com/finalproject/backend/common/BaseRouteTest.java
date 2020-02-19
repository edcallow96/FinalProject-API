package com.finalproject.backend.common;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.finalproject.backend.fileidentification.FileIdentificationProcessor;
import com.finalproject.backend.handlers.PrepareJobProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
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

import java.io.IOException;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;

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

  @EndpointInject("mock:" + PROCESS_JOB)
  protected MockEndpoint mockProcessJobEndpoint;

  @Autowired
  protected ProducerTemplate templateProducer;

  @Autowired
  protected CamelContext camelContext;

  @MockBean
  protected AmazonS3 amazonS3;

  @MockBean(name = "amazonDynamoDB")
  protected AmazonDynamoDB amazonDynamoDB;

  @MockBean
  protected FileIdentificationProcessor fileIdentificationProcessor;

  @MockBean
  protected PrepareJobProcessor prepareJobProcessor;

  protected S3Event s3Event;

  protected Exchange exchange;

  @Before
  public void setUp() throws IOException {
    exchange = ExchangeBuilder.anExchange(camelContext).build();
  }
}