package com.finalproject.backend.archive;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.tika.mime.MediaType;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;

public class ZipFilePredicateShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks
  private ZipFilePredicate zipFilePredicate;

  private Exchange exchange;

  @Before
  public void setUp() {
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(ProcessJob.builder().contentType(MediaType.OCTET_STREAM).build()).build();
  }

  @Test
  public void returnFalseForNonZipContentTypes() {
    assertThat(zipFilePredicate.matches(exchange), Matchers.is(false));
  }

  @Test
  public void returnTrueForZipContentType() {
    exchange.getIn().getBody(ProcessJob.class).setContentType(MediaType.APPLICATION_ZIP);
    assertThat(zipFilePredicate.matches(exchange), Matchers.is(true));
  }

}