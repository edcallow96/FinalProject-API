package com.finalproject.backend.archive;

import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class UnZipProcessor implements Processor {

  @Override
  public void process(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    Path destinationDirectory = processJob.getPayloadLocation().toPath().getParent().resolve(
        processJob.getPayloadLocation().getName() + "_extracted");
    log.info("Attempting to extract {} to {}", processJob.getPayloadLocation(), destinationDirectory);
    try {
      ZipFile zipFile = new ZipFile(processJob.getPayloadLocation());
      zipFile.extractAll(destinationDirectory.toString());
      List<ProcessJob> extractedFiles = getExtractedFiles(destinationDirectory, processJob);
      log.info("Extracted {} files from {}", extractedFiles.size(), zipFile);
      exchange.getIn().setBody(extractedFiles);
    } catch (Exception e) {
      log.error("UnZipping file {} failed", processJob.getPayloadLocation(), e);
    }
  }

  private List<ProcessJob> getExtractedFiles(Path destinationDirectory, ProcessJob processJob) {
    return asList(destinationDirectory.toFile().listFiles()).stream().map(
        extractedFile -> ProcessJob.builder()
            .payloadLocation(extractedFile)
            .user(processJob.getUser())
            .originalFileHash(processJob.getOriginalFileHash())
            .originalFileSize(processJob.getOriginalFileSize())
            .jobId(processJob.getJobId())
            .sourceBucket(processJob.getSourceBucket())
            .sourceKey(processJob.getSourceKey())
            .build())
        .collect(toList());
  }
}
