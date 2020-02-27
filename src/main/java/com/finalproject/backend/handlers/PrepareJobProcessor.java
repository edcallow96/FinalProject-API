package com.finalproject.backend.handlers;

import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.userservice.UserNotFoundException;
import com.finalproject.backend.userservice.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static com.finalproject.backend.constants.BackendApplicationConstants.AMAZON_REQUEST_ID;
import static com.finalproject.backend.constants.BackendApplicationConstants.AMZ_METADATA_USER_ID;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component
@Slf4j
public class PrepareJobProcessor implements Processor {

  private final ApplicationProperties applicationProperties;
  private final UserRepository userRepository;

  public PrepareJobProcessor(final ApplicationProperties applicationProperties, final UserRepository userRepository) {
    this.applicationProperties = applicationProperties;
    this.userRepository = userRepository;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    S3Object s3Object = exchange.getIn().getBody(S3Object.class);
    String amazonRequestId = exchange.getIn().getHeader(AMAZON_REQUEST_ID, String.class);
    log.info("Preparing Job with Id {}", amazonRequestId);
    File downloadedFile = downloadS3Object(s3Object, amazonRequestId);
    exchange.getIn().setBody(buildJob(amazonRequestId, downloadedFile, s3Object));
  }

  private File downloadS3Object(S3Object s3Object, String amazonRequestId) throws IOException {
    Path downloadDirectory = applicationProperties.getDownloadDirectory().resolve(amazonRequestId);
    downloadDirectory.toFile().mkdirs();
    File downloadedFile = downloadDirectory.resolve(s3Object.getKey()).toFile();
    log.info("Downloading S3 file contents to {}", downloadedFile);
    FileUtils.copyInputStreamToFile(s3Object.getObjectContent(), downloadedFile);
    log.info("Downloaded {} bytes of file content", downloadedFile.length());
    return downloadedFile;
  }

  private ProcessJob buildJob(String amazonRequestId, File downloadedFile, S3Object s3Object) throws Exception {
    String userId = s3Object.getObjectMetadata().getUserMetaDataOf(AMZ_METADATA_USER_ID);
    return ProcessJob.builder()
        .jobId(amazonRequestId)
        .payloadLocation(downloadedFile)
        .originalFileHash(md5Hex(new FileInputStream(downloadedFile)).toUpperCase())
        .originalFileSize(downloadedFile.length())
        .sourceBucket(s3Object.getBucketName())
        .sourceKey(s3Object.getKey())
        .user(userRepository.findById(userId).orElseThrow(() ->
            new UserNotFoundException(String.format("User %s does not exist in User Service", userId))))
        .build();
  }
}
