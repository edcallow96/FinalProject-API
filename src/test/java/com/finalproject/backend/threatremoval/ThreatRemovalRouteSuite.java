package com.finalproject.backend.threatremoval;


import com.finalproject.backend.common.BaseRouteTest;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.finalproject.backend.constants.BackendApplicationConstants.THREAT_REMOVAL_ROUTE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockEndpoints(THREAT_REMOVAL_ROUTE)
public class ThreatRemovalRouteSuite extends BaseRouteTest {

  @EndpointInject("mock:" + THREAT_REMOVAL_ROUTE)
  private MockEndpoint mockThreatRemovalEndpoint;

  @MockBean
  private ThreatRemovalProcessor threatRemovalProcessor;

  @MockBean
  private SupportedThreatRemovalTypePredicate supportedThreatRemovalTypePredicate;

  @Test
  public void threatRemovalRouteShouldCallProcessorWhenFileTypeSupported() {
    when(supportedThreatRemovalTypePredicate.matches(exchange)).thenReturn(true);

    templateProducer.send(THREAT_REMOVAL_ROUTE, exchange);

    verify(supportedThreatRemovalTypePredicate).matches(exchange);
    verify(threatRemovalProcessor).process(exchange);
  }
}