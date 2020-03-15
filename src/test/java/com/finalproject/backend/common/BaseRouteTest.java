package com.finalproject.backend.common;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpointsAndSkip;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.finalproject.backend.constants.BackendApplicationConstants.AMAZON_REQUEST_ID;
import static com.finalproject.backend.constants.BackendApplicationConstants.UNZIP_FILE_ROUTE;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@MockEndpointsAndSkip
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseRouteTest {

  @EndpointInject("mock:" + UNZIP_FILE_ROUTE)
  protected MockEndpoint mockUnzipFileEndpoint;

  @Autowired
  protected ProducerTemplate templateProducer;

  @Autowired
  protected CamelContext camelContext;

  @MockBean
  protected AmazonS3 amazonS3;

  @MockBean(name = "amazonDynamoDB")
  protected AmazonDynamoDB amazonDynamoDB;

  @MockBean
  private AmazonSimpleEmailService simpleEmailService;

  protected S3Event s3Event;

  protected Exchange exchange;

  @Before
  public void setUp() throws Exception {
    exchange = ExchangeBuilder.anExchange(camelContext).withHeader(AMAZON_REQUEST_ID, randomAlphabetic(10)).withBody(ProcessJob.builder().build()).build();
  }

}