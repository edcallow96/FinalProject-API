package com.finalproject.backend.routes;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.handlers.FinalProjectLambdaFunction;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;

import static com.finalproject.backend.constants.BackendApplicationConstants.ENTRY_POINT_ROUTE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@MockEndpoints
public class LambdaEntryPointRouteShould {

  @EndpointInject("mock:" + ENTRY_POINT_ROUTE)
  private MockEndpoint mockEntryPointRoute;

  @Autowired
  private FinalProjectLambdaFunction finalProjectLambdaFunction;

  @MockBean
  private AmazonS3 amazonS3;

  @Before
  public void setUp() {
    when(amazonS3.getObject(anyString(), anyString())).thenReturn(new S3Object());
  }

  @Test
  public void test() throws IOException {
    S3EventNotification s3EventNotification = S3Event.parseJson(FileUtils.readFileToString(new File("src/test/resources/event.json"), UTF_8));
    S3Event event = new S3Event(s3EventNotification.getRecords());
    finalProjectLambdaFunction.apply(event);
  }


}