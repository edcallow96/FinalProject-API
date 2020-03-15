package com.finalproject.backend.antivirus;

import com.finalproject.backend.common.BaseRouteTest;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.finalproject.backend.constants.BackendApplicationConstants.ANTI_VIRUS_SCANNING_ROUTE;
import static org.mockito.Mockito.verify;

@MockEndpoints(ANTI_VIRUS_SCANNING_ROUTE)
public class AntiVirusScanningRouteRouteSuite extends BaseRouteTest {

  @MockBean
  private AntiVirusProcessor antiVirusProcessor;

  @Test
  public void antiVirusRouteShouldCallProcessor() {
    templateProducer.send(ANTI_VIRUS_SCANNING_ROUTE, exchange);

    verify(antiVirusProcessor).process(exchange);
  }

}