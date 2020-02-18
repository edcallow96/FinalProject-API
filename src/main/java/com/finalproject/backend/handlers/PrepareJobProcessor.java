package com.finalproject.backend.handlers;

import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static com.finalproject.backend.constants.BackendApplicationConstants.AMAZON_REQUEST_ID;

@Component
@Slf4j
public class PrepareJobProcessor implements Processor {

  private final ApplicationProperties applicationProperties;

  public PrepareJobProcessor(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    S3Object s3Object = exchange.getIn().getBody(S3Object.class);
    String amazonRequestId = exchange.getIn().getHeader(AMAZON_REQUEST_ID, String.class);
    log.info("Preparing Job with Id {}", amazonRequestId);
    File downloadedFile = downloadS3Object(s3Object);
    exchange.getIn().setBody(buildJob(amazonRequestId, downloadedFile, s3Object));
  }

  private File downloadS3Object(S3Object s3Object) throws IOException {
    Path downloadDirectory = applicationProperties.getDownloadDirectory().resolve(UUID.randomUUID().toString());
    downloadDirectory.toFile().mkdirs();
    File downloadedFile = downloadDirectory.resolve(s3Object.getKey()).toFile();
    log.info("Downloading S3 file contents to {}", downloadedFile);
    FileUtils.copyInputStreamToFile(s3Object.getObjectContent(), downloadedFile);
    log.info("Downloaded {} bytes of file content", downloadedFile.length());
    return downloadedFile;
  }

  private ProcessJob buildJob(String amazonRequestId, File downloadedFile, S3Object s3Object) {
    return ProcessJob.builder().jobId(amazonRequestId).payloadLocation(downloadedFile).sourceBucket(s3Object.getBucketName()).sourceKey(s3Object.getKey()).build();
  }
}
