package com.finalproject.backend.notification;

import com.amazonaws.services.s3.AmazonS3;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UploadTreatedFileProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private AmazonS3 amazonS3;

  @Mock
  private ApplicationProperties applicationProperties;

  @InjectMocks
  private UploadTreatedFileProcessor uploadTreatedFileProcessor;

  private Exchange exchange;

  @Before
  public void setUp() throws IOException {
    when(applicationProperties.getTreatedBucketName()).thenReturn(randomAlphabetic(10));
    when(applicationProperties.getDownloadDirectory()).thenReturn(temporaryFolder.newFolder(randomAlphabetic(10)).toPath());
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(
        ProcessJob.builder()
            .payloadLocation(applicationProperties.getDownloadDirectory().resolve(randomAlphabetic(10)).toFile())
            .build()
    ).build();
  }

  @Test
  public void uploadsProcessedFileToTreatedBucket() {
    uploadTreatedFileProcessor.process(exchange);

    File processedFile = exchange.getIn().getBody(ProcessJob.class).getPayloadLocation();

    verify(amazonS3).putObject(applicationProperties.getTreatedBucketName(), processedFile.getName(), processedFile);
  }

  @Test
  public void updateProcessJobWithKeyInTreatedBucket() {
    uploadTreatedFileProcessor.process(exchange);

    String expectedKey = exchange.getIn().getBody(ProcessJob.class).getPayloadLocation().getName();

    assertThat(exchange.getIn().getBody(ProcessJob.class).getTreatedBucketKey(), equalTo(expectedKey));
  }

}