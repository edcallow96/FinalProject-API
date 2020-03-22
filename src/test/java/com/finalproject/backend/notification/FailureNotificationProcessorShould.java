package com.finalproject.backend.notification;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import com.finalproject.backend.model.User;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.tika.mime.MediaType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.finalproject.backend.model.ProcessName.FILE_IDENTIFICATION;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

public class FailureNotificationProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationProperties applicationProperties;

  @Mock
  private AmazonSimpleEmailService amazonSimpleEmailService;

  @InjectMocks
  private FailureNotificationProcessor failureNotificationProcessor;

  @Captor
  private ArgumentCaptor<SendEmailRequest> emailRequestArgumentCaptor;

  private Exchange exchange;

  @Before
  public void setUp() throws IOException {
    ProcessJob processJob = ProcessJob.builder()
        .jobId(randomAlphabetic(10))
        .originalFileSize(new Random().nextLong())
        .originalFileHash(randomAlphabetic(10))
        .payloadLocation(temporaryFolder.newFile(randomAlphabetic(10)))
        .user(User.builder().firstName(randomAlphabetic(10)).lastName(randomAlphabetic(10)).emailAddress(randomAlphabetic(10)).build())
        .processingResults(asList(ProcessResult.builder().processStatus(FAILED).processName(FILE_IDENTIFICATION).failureReason(randomAlphabetic(10)).build()))
        .contentType(MediaType.TEXT_PLAIN)
        .sourceKey(randomAlphabetic(10))
        .treatedBucketKey(randomAlphabetic(10))
        .build();

    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(processJob).build();
  }

  @Test
  public void generateEmailHeaderThatContainsUsersFirstName() {
    failureNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    String firstName = exchange.getIn().getBody(ProcessJob.class).getUser().getFirstName();
    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().selectFirst("h1").toString(), containsString(firstName));
  }

  @Test
  public void generateEmailContaningTheFailureReason() {
    failureNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    String failureReason = exchange.getIn().getBody(ProcessJob.class).getProcessingResults().get(0).getFailureReason();

    assertThat(document.body().select("p:nth-of-type(5)").toString(), containsString(failureReason));
  }

  @Test
  public void generateEmailContentThatContainsJobId() {
    failureNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    String jobId = exchange.getIn().getBody(ProcessJob.class).getJobId();
    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().selectFirst("h2").toString(), containsString(jobId));
  }

  @Test
  public void generateEmailContentThatContainsOriginalFileInformation() {
    failureNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().select("p:nth-of-type(1)").toString(), containsString(processJob.getSourceKey()));
    assertThat(document.body().select("p:nth-of-type(2)").toString(), containsString(processJob.getContentType().toString()));
    assertThat(document.body().select("p:nth-of-type(3)").toString(), containsString(processJob.getOriginalFileHash()));
    assertThat(document.body().select("p:nth-of-type(4)").toString(), containsString(Long.toString(processJob.getOriginalFileSize())));
  }

  @Test
  public void generateEmailWithProcessingResultsTable() {
    failureNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    List<ProcessResult> processResults = exchange.getIn().getBody(ProcessJob.class).getProcessingResults();
    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.select("caption").html(), equalTo("Processing Results"));
    for (int i = 0; i < processResults.size(); i++) {
      assertThat(document.select(format("tr:nth-of-type(%s) > td:nth-of-type(1)", i + 2)).html(), equalTo(processResults.get(i).getProcessName().name()));
      assertThat(document.select(format("tr:nth-of-type(%s) > td:nth-of-type(2)", i + 2)).html(), equalTo(processResults.get(i).getProcessStatus().name()));
    }
  }

  @Test
  public void generateGenericFailureEmailWhenReasonIsUnknown() {
    exchange.getIn().getBody(ProcessJob.class).setProcessingResults(new ArrayList<>());

    failureNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();
    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().select("p:nth-of-type(5)").toString(), containsString("an unexpected failure occurred"));
  }

}