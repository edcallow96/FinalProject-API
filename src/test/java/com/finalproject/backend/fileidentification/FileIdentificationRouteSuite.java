package com.finalproject.backend.fileidentification;

import com.finalproject.backend.common.BaseRouteTest;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.finalproject.backend.constants.BackendApplicationConstants.FILE_IDENTIFICATION_ROUTE;
import static org.mockito.Mockito.verify;

@MockEndpoints(FILE_IDENTIFICATION_ROUTE)
public class FileIdentificationRouteSuite extends BaseRouteTest {

  @MockBean
  private FileIdentificationProcessor fileIdentificationProcessor;

  @Test
  public void fileIdentificationRouteShouldCallProcessor() {
    templateProducer.send(FILE_IDENTIFICATION_ROUTE, exchange);

    verify(fileIdentificationProcessor).process(exchange);
  }
}