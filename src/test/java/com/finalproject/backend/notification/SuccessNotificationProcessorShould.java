package com.finalproject.backend.notification;

import com.amazonaws.services.s3.AmazonS3;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static com.finalproject.backend.model.ProcessName.FILE_IDENTIFICATION;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SuccessNotificationProcessorShould {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private AmazonS3 amazonS3;

  @Mock
  private ApplicationProperties applicationProperties;

  @Mock
  private AmazonSimpleEmailService amazonSimpleEmailService;

  @InjectMocks
  private SuccessNotificationProcessor successNotificationProcessor;

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
        .processingResults(asList(ProcessResult.builder().processStatus(SUCCESS).processName(FILE_IDENTIFICATION).build()))
        .contentType(MediaType.TEXT_PLAIN)
        .treatedBucketKey(randomAlphabetic(10))
        .build();
    exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).withBody(processJob).build();

    when(applicationProperties.getSelfSignedUrlExpirationDays()).thenReturn(new Random().nextInt());
    when(applicationProperties.getTreatedBucketName()).thenReturn(randomAlphabetic(10));
    when(applicationProperties.getNotificationSenderAddress()).thenReturn(randomAlphabetic(10));

    when(amazonS3.generatePresignedUrl(anyString(), anyString(), any(Date.class))).thenReturn(new URL("http://" + randomAlphabetic(10)));


  }

  @Test
  public void generatePreSignedUrl() {
    successNotificationProcessor.process(exchange);

    String bucketName = applicationProperties.getTreatedBucketName();
    String bucketKey = exchange.getIn().getBody(ProcessJob.class).getTreatedBucketKey();

    verify(amazonS3).generatePresignedUrl(eq(bucketName), eq(bucketKey), any(Date.class));
  }

  @Test
  public void sendEmailNotificationToUserAddress() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    String expectedRecipientAddress = exchange.getIn().getBody(ProcessJob.class).getUser().getEmailAddress();
    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    assertThat(singletonList(expectedRecipientAddress), equalTo(capturedEmailRequest.getDestination().getToAddresses()));
  }

  @Test
  public void sendEmailNotificationFromConfiguredAddress() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    String expectedSenderAddress = applicationProperties.getNotificationSenderAddress();
    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    assertThat(expectedSenderAddress, equalTo(capturedEmailRequest.getSource()));
  }

  @Test
  public void generateHtmlEmailContent() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document, notNullValue());
    assertThat(document.body(), notNullValue());
    assertThat(document.body().getAllElements(), not(empty()));
  }

  @Test
  public void generateEmailHeaderThatContainsUsersFirstName() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    String firstName = exchange.getIn().getBody(ProcessJob.class).getUser().getFirstName();
    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().selectFirst("h1").toString(), containsString(firstName));
  }

  @Test
  public void generateEmailContentThatContainsJobId() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    String jobId = exchange.getIn().getBody(ProcessJob.class).getJobId();
    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().selectFirst("h2").toString(), containsString(jobId));
  }

  @Test
  public void generateEmailContentThatContainsOriginalFileInformation() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().select("p:nth-of-type(1)").toString(), containsString(processJob.getPayloadLocation().getName()));
    assertThat(document.body().select("p:nth-of-type(2)").toString(), containsString(processJob.getOriginalFileHash()));
    assertThat(document.body().select("p:nth-of-type(3)").toString(), containsString(Long.toString(processJob.getOriginalFileSize())));
  }

  @Test
  public void generateEmailContentThatContainsProcessedFileInformation() throws IOException {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().select("p:nth-of-type(4)").toString(), containsString(md5Hex(new FileInputStream(processJob.getPayloadLocation())).toUpperCase()));
    assertThat(document.body().select("p:nth-of-type(5)").toString(), containsString(Long.toString(processJob.getPayloadLocation().length())));
  }

  @Test
  public void notIncludeProcessFileInformationWhenFileHasNotBeenAltered() throws IOException {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    processJob.setOriginalFileHash(md5Hex(new FileInputStream(processJob.getPayloadLocation())).toUpperCase());
    exchange.getIn().setBody(processJob);

    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().select("p:nth-of-type(4)").toString(), not(containsString(md5Hex(new FileInputStream(processJob.getPayloadLocation())).toUpperCase())));
    assertThat(document.body().select("p:nth-of-type(5)").toString(), not(containsString(Long.toString(processJob.getPayloadLocation().length()))));
  }


  @Test
  public void generateEmailWithProcessingResultsTable() {
    successNotificationProcessor.process(exchange);

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
  public void generateEmailWithPreSignedUrlLink() throws MalformedURLException {
    String expectedPreSignedUrl = "http://" + randomAlphabetic(10);
    when(amazonS3.generatePresignedUrl(anyString(), anyString(), any(Date.class))).thenReturn(new URL(expectedPreSignedUrl));

    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.select("a").attr("href"), equalTo(expectedPreSignedUrl));
  }

  @Test
  public void generateEmailWithLinkExpiration() {
    successNotificationProcessor.process(exchange);

    verify(amazonSimpleEmailService).sendEmail(emailRequestArgumentCaptor.capture());

    SendEmailRequest capturedEmailRequest = emailRequestArgumentCaptor.getValue();

    Document document = Jsoup.parse(capturedEmailRequest.getMessage().getBody().getHtml().getData());

    assertThat(document.body().select("p:nth-of-type(6)").toString(),
        containsString(format("This link will be valid for %s days.", applicationProperties.getSelfSignedUrlExpirationDays())));
  }


}