package com.finalproject.backend.notification;

import com.amazonaws.services.s3.AmazonS3;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UploadTreatedFileProcessor implements Processor {

  private final ApplicationProperties applicationProperties;
  private AmazonS3 amazonS3;

  public UploadTreatedFileProcessor(final ApplicationProperties applicationProperties, final AmazonS3 amazonS3) {
    this.applicationProperties = applicationProperties;
    this.amazonS3 = amazonS3;
  }

  @Override
  public void process(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    String locationWithinBucket = applicationProperties.getDownloadDirectory().relativize(processJob.getPayloadLocation().toPath()).toString();
    log.info("Location within bucket {}", locationWithinBucket);
    amazonS3.putObject(applicationProperties.getTreatedBucketName(), locationWithinBucket, processJob.getPayloadLocation());
    processJob.setTreatedBucketKey(locationWithinBucket);
  }
}
