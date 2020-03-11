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
import java.net.URL;
import java.util.Date;
import java.util.Random;

import static com.finalproject.backend.model.ProcessName.FILE_IDENTIFICATION;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
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

}