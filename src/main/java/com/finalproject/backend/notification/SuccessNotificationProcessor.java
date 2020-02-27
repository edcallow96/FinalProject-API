package com.finalproject.backend.notification;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Date;

import static j2html.TagCreator.*;
import static java.lang.String.format;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component
@Slf4j
public class SuccessNotificationProcessor implements Processor {

  private final AmazonS3 amazonS3;
  private final ApplicationProperties applicationProperties;
  private final AmazonSimpleEmailService emailService;

  public SuccessNotificationProcessor(final AmazonS3 amazonS3,
                                      final ApplicationProperties applicationProperties,
                                      final AmazonSimpleEmailService emailService) {
    this.amazonS3 = amazonS3;
    this.applicationProperties = applicationProperties;
    this.emailService = emailService;
  }

  @Override
  public void process(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    URL preSignedUrl = generatePreSignedUrl(processJob);
    log.info("Generated pre-signed URL: {}", preSignedUrl);
    log.info("Sending success notification for Job {}", processJob.getJobId());
    try {
      sendSuccessNotification(processJob, preSignedUrl);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private URL generatePreSignedUrl(ProcessJob processJob) {
    Date expirationDate = DateUtils.addDays(new Date(), applicationProperties.getSelfSignedUrlExpirationDays());
    log.info("Generating pre-signed URL for key {} in bucket {}. Expiration date: {}",
        processJob.getTreatedBucketKey(), applicationProperties.getTreatedBucketName(), expirationDate);
    return amazonS3.generatePresignedUrl(applicationProperties.getTreatedBucketName(), processJob.getTreatedBucketKey(), expirationDate);
  }

  private void sendSuccessNotification(ProcessJob processJob, URL preSignedUrl) throws Exception {
    log.info("Sending success notification to {}", processJob.getUser().getEmailAddress());
    SendEmailRequest request = new SendEmailRequest()
        .withDestination(
            new Destination().withToAddresses(processJob.getUser().getEmailAddress()))
        .withMessage(new Message()
            .withBody(new Body()
                .withHtml(new Content()
                    .withCharset(UTF_8).withData(generateHtmlBody(processJob, preSignedUrl))))
            .withSubject(new Content()
                .withCharset(UTF_8).withData("Your file has been successfully processed!")))
        .withSource(applicationProperties.getNotificationSenderAddress());
    log.info("Request: {}", request);
    emailService.sendEmail(request);
  }

  private String generateHtmlBody(ProcessJob processJob, URL preSignedUrl) throws Exception {
    return body(
        h1(format("Hello, %s! Here is the link to your processed file:", processJob.getUser().getFirstName())),
        h2(format("File name: %s", processJob.getPayloadLocation().getName())),
        h3(format("Original file hash: %s", processJob.getOriginalFileHash())),
        h3(format("Original file size: %s", processJob.getOriginalFileSize())),
        br(),
        h3(format("Processed file hash: %s", md5Hex(new FileInputStream(processJob.getPayloadLocation())).toUpperCase())),
        h3(format("Processed file size: %s", processJob.getPayloadLocation().length())),
        p("Click ").with(a("here").withHref(preSignedUrl.toString())).withText(" to download the processed file.")
            .withText(format(" This link will be valid for %s days.", applicationProperties.getSelfSignedUrlExpirationDays()))
    ).render();
  }

}
