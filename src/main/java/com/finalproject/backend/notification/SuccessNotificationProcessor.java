package com.finalproject.backend.notification;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Date;

import static j2html.TagCreator.*;
import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component
@Slf4j
public class SuccessNotificationProcessor extends BaseNotificationProcessor {

  private final AmazonS3 amazonS3;
  private final ApplicationProperties applicationProperties;

  public SuccessNotificationProcessor(final AmazonS3 amazonS3,
                                      final ApplicationProperties applicationProperties,
                                      final AmazonSimpleEmailService amazonSimpleEmailService) {
    super(amazonSimpleEmailService, applicationProperties);
    this.amazonS3 = amazonS3;
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void process(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    URL preSignedUrl = generatePreSignedUrl(processJob);
    log.info("Generated pre-signed URL: {}", preSignedUrl);
    log.info("Sending success notification for Job {}", processJob.getJobId());
    try {
      sendNotification("Your file has been successfully processed!",
          generateHtmlBody(processJob, preSignedUrl), processJob.getUser().getEmailAddress());
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
