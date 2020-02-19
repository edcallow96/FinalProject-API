package com.finalproject.backend.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.User;
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
import static com.finalproject.backend.constants.BackendApplicationConstants.AMZ_METADATA_USER_ID;

@Component
@Slf4j
public class PrepareJobProcessor implements Processor {

  private final ApplicationProperties applicationProperties;

  private final AmazonDynamoDB amazonDynamoDB;

  public PrepareJobProcessor(final ApplicationProperties applicationProperties,
                             final AmazonDynamoDB amazonDynamoDB) {
    this.applicationProperties = applicationProperties;
    this.amazonDynamoDB = amazonDynamoDB;
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
    String userId = s3Object.getObjectMetadata().getUserMetaDataOf(AMZ_METADATA_USER_ID);
    return ProcessJob.builder()
        .jobId(amazonRequestId)
        .payloadLocation(downloadedFile)
        .sourceBucket(s3Object.getBucketName())
        .sourceKey(s3Object.getKey())
        .user(getUser(userId))
        .build();
  }

  private User getUser(String userId) {
    DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
    Table table = dynamoDB.getTable("UserTable");
    Item item = table.getItem("userId", userId);
    return User.builder().userId(userId).emailAddress((String) item.get("emailAddress")).build();
  }
}
