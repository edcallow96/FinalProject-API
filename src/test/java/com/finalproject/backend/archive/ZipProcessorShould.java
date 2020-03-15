package com.finalproject.backend.archive;

import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import net.lingala.zip4j.ZipFile;
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
import java.util.ArrayList;
import java.util.List;

import static com.finalproject.backend.constants.BackendApplicationConstants.EXTRACTED_FILE_RESULTS;
import static com.finalproject.backend.model.ProcessName.ANTI_VIRUS_SCAN;
import static com.finalproject.backend.model.ProcessName.FILE_IDENTIFICATION;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.mockito.Mockito.when;

public class ZipProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationProperties applicationProperties;

  @InjectMocks
  private ZipProcessor zipProcessor;

  private Exchange exchange;

  @Before
  public void setUp() throws IOException {
    when(applicationProperties.getDownloadDirectory()).thenReturn(temporaryFolder.newFolder(randomAlphabetic(10)).toPath());
    List<ProcessJob> processJobs = new ArrayList<>();
    processJobs.add(ProcessJob.builder()
        .payloadLocation(temporaryFolder.newFile(randomAlphabetic(10)))
        .sourceKey(randomAlphabetic(10))
        .jobId(randomAlphabetic(10))
        .processingResults(asList(
            ProcessResult.builder().processName(FILE_IDENTIFICATION).processStatus(SUCCESS).build(),
            ProcessResult.builder().processName(ANTI_VIRUS_SCAN).processStatus(SUCCESS).build()
        ))
        .build());
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(processJobs).build();
  }

  @Test
  public void zipProcessedFiles() throws Exception {
    zipProcessor.process(exchange);

    File zippedFile = exchange.getIn().getBody(ProcessJob.class).getPayloadLocation();

    assertThat(zippedFile, anExistingFile());
    assertThat(new ZipFile(zippedFile).getSplitZipFiles(), hasSize(1));
  }

  @Test
  public void aggregatesSuccessfulProcessingResults() throws Exception {
    zipProcessor.process(exchange);

    ProcessJob aggregatedProcessJob = exchange.getIn().getBody(ProcessJob.class);

    assertThat(aggregatedProcessJob.getProcessingResults(), hasSize(2));
    aggregatedProcessJob.getProcessingResults().forEach(result -> assertThat(result.getProcessStatus(), equalTo(SUCCESS)));
  }

  @Test
  public void aggregateFailedProcessingResults() throws Exception {
    ((ProcessJob) exchange.getIn().getBody(List.class).get(0)).setProcessingResults(
        singletonList(ProcessResult.builder().processName(FILE_IDENTIFICATION).processStatus(FAILED).build()));

    zipProcessor.process(exchange);

    ProcessJob aggregatedProcessJob = exchange.getIn().getBody(ProcessJob.class);

    assertThat(aggregatedProcessJob.getProcessingResults(), hasSize(1));
    assertThat(aggregatedProcessJob.getProcessingResults().get(0).getProcessStatus(), equalTo(FAILED));
  }

  @Test
  public void setExtractedFileResultsAsExchangeHeader() throws Exception {
    List<ProcessJob> extractedFileResults = exchange.getIn().getBody(List.class);

    zipProcessor.process(exchange);

    assertThat(exchange.getIn().getHeader(EXTRACTED_FILE_RESULTS), equalTo(extractedFileResults));
  }

}