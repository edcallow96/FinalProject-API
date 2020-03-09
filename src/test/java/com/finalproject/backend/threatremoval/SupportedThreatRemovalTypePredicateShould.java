package com.finalproject.backend.threatremoval;

import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.tika.mime.MediaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.finalproject.backend.constants.BackendApplicationConstants.BYTES_PER_MEGABYTE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class SupportedThreatRemovalTypePredicateShould {

  @Mock
  private ApplicationProperties applicationProperties;

  @InjectMocks
  private SupportedThreatRemovalTypePredicate supportedThreatRemovalTypePredicate;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private Exchange exchange;

  @Before
  public void setUp() {
    when(applicationProperties.getMaxThreatRemovalFileSize()).thenReturn(4.5);
    ProcessJob processJob = ProcessJob.builder().originalFileSize(BYTES_PER_MEGABYTE).contentType(MediaType.parse("application/pdf")).build();
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(processJob).build();
  }

  @Test
  public void matchSupportedContentTypesAndFileSizes() {
    assertThat(supportedThreatRemovalTypePredicate.matches(exchange), equalTo(true));
  }

  @Test
  public void matchSupportedContentTypesWithMaxFileSize() {
    exchange.getIn().getBody(ProcessJob.class).setOriginalFileSize((long) (4.5 * BYTES_PER_MEGABYTE));

    assertThat(supportedThreatRemovalTypePredicate.matches(exchange), equalTo(true));
  }

  @Test
  public void notMatchUnSupportedContentTypes() {
    exchange.getIn().getBody(ProcessJob.class).setContentType(MediaType.TEXT_PLAIN);

    assertThat(supportedThreatRemovalTypePredicate.matches(exchange), equalTo(false));
  }

  @Test
  public void notMatchUnSupportedFileSizes() {
    exchange.getIn().getBody(ProcessJob.class).setOriginalFileSize(5 * BYTES_PER_MEGABYTE);

    assertThat(supportedThreatRemovalTypePredicate.matches(exchange), equalTo(false));
  }

  @Test
  public void notMatchUnSupportedFileSizesAndContentTypes() {
    exchange.getIn().getBody(ProcessJob.class).setOriginalFileSize(5 * BYTES_PER_MEGABYTE);
    exchange.getIn().getBody(ProcessJob.class).setContentType(MediaType.TEXT_PLAIN);

    assertThat(supportedThreatRemovalTypePredicate.matches(exchange), equalTo(false));
  }
}