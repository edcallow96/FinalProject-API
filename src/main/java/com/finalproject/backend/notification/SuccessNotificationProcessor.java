package com.finalproject.backend.notification;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import j2html.tags.DomContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static j2html.TagCreator.*;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component
@Slf4j
public class SuccessNotificationProcessor extends BaseNotificationProcessor {

  private final AmazonS3 amazonS3;

  public SuccessNotificationProcessor(final AmazonS3 amazonS3,
                                      final ApplicationProperties applicationProperties,
                                      final AmazonSimpleEmailService amazonSimpleEmailService) {
    super(amazonSimpleEmailService, applicationProperties);
    this.amazonS3 = amazonS3;
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
      log.error("Sending notification failed.", exception);
    }
  }

  private URL generatePreSignedUrl(ProcessJob processJob) {
    Date expirationDate = DateUtils.addDays(new Date(), applicationProperties.getSelfSignedUrlExpirationDays());
    log.info("Generating pre-signed URL for key {} in bucket {}. Expiration date: {}",
        processJob.getTreatedBucketKey(), applicationProperties.getTreatedBucketName(), expirationDate);
    return amazonS3.generatePresignedUrl(applicationProperties.getTreatedBucketName(), processJob.getTreatedBucketKey(), expirationDate);
  }

  private String generateHtmlBody(ProcessJob processJob, URL preSignedUrl) throws IOException {
    List<DomContent> bodyContents = new ArrayList<>(Arrays.asList(
        style(IOUtils.toString(getClass().getResourceAsStream("/emailCss.css"), UTF_8)),
        h1(format("Hello, %s! Here is the link to your processed file:", processJob.getUser().getFirstName())),
        h2(format("Job Id: %s", processJob.getJobId()))));

    bodyContents.addAll(getFileInfoSection(processJob));

    String newFileHash = md5Hex(new FileInputStream(processJob.getPayloadLocation())).toUpperCase();
    if (!newFileHash.equals(processJob.getOriginalFileHash())) {
      bodyContents.add(p(format("Processed file hash: %s", newFileHash)));
      bodyContents.add(p(format("Processed file size: %s", processJob.getPayloadLocation().length())));
    }

    bodyContents.add(getProcessingResultsTable(processJob));

    bodyContents.add(p("Click ").with(a("here").withHref(preSignedUrl.toString())).withText(" to download the processed file.")
        .withText(format(" This link will be valid for %s days.", applicationProperties.getSelfSignedUrlExpirationDays())));
    return body(bodyContents.toArray(new DomContent[0])).render();
  }

}
