package com.finalproject.backend.fileidentification;

import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessName;
import com.finalproject.backend.model.ProcessStatus;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.tika.mime.MediaType;
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
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FileIdentificationProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private FileIdentifier fileIdentifier;

  @InjectMocks
  private FileIdentificationProcessor fileIdentificationProcessor;

  private Exchange exchange;

  @Before
  public void setUp() throws FileIdentificationException, IOException {
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(
        ProcessJob.builder().payloadLocation(temporaryFolder.newFile(randomAlphabetic(10))).build()).build();
    when(fileIdentifier.identifyFile(any(File.class))).thenReturn(MediaType.OCTET_STREAM);
  }

  @Test
  public void populateProcessJobContentTypeWithIdentifiedFileType() {
    fileIdentificationProcessor.process(exchange);

    assertThat(exchange.getIn().getBody(ProcessJob.class).getContentType(), equalTo(MediaType.OCTET_STREAM));
  }

  @Test
  public void addSuccessProcessingResult() {
    fileIdentificationProcessor.process(exchange);

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    assertThat(processJob.getProcessingResults(), hasSize(1));
    assertThat(processJob.getProcessingResults().get(0).getProcessName(), equalTo(ProcessName.FILE_IDENTIFICATION));
    assertThat(processJob.getProcessingResults().get(0).getProcessStatus(), equalTo(ProcessStatus.SUCCESS));
  }

  @Test
  public void addFailureProcessingResultWhenFileIdentificationFails() throws FileIdentificationException {
    when(fileIdentifier.identifyFile(any(File.class))).thenThrow(new RuntimeException());
    fileIdentificationProcessor.process(exchange);

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    assertThat(processJob.getProcessingResults(), hasSize(1));
    assertThat(processJob.getProcessingResults().get(0).getProcessName(), equalTo(ProcessName.FILE_IDENTIFICATION));
    assertThat(processJob.getProcessingResults().get(0).getProcessStatus(), equalTo(ProcessStatus.FAILED));
  }

}