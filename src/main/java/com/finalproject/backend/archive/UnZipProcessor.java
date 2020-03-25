package com.finalproject.backend.archive;

import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.finalproject.backend.model.ProcessName.UNZIP;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class UnZipProcessor extends PayloadProcessor {

  @Override
  public void process(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    try {
      processCurrentJob(processJob);
      List<ProcessJob> extractedFiles = getExtractedFiles(resolveExtractionDirectory(processJob), processJob);
      extractedFiles.forEach(this::succeedCurrentJob);
      exchange.getIn().setBody(extractedFiles);
    } catch (Exception e) {
      log.error("UnZipping file {} failed", processJob.getPayloadLocation(), e);
      failCurrentJob(processJob, e.getMessage());
    }
  }

  @Override
  @SneakyThrows
  protected void processCurrentJob(ProcessJob currentProcessJob) {
    Path destinationDirectory = resolveExtractionDirectory(currentProcessJob);
    log.info("Attempting to extract {} to {}", currentProcessJob.getPayloadLocation(), destinationDirectory);
    ZipFile zipFile = new ZipFile(currentProcessJob.getPayloadLocation());
    zipFile.extractAll(destinationDirectory.toString());
    List<ProcessJob> extractedFiles = getExtractedFiles(destinationDirectory, currentProcessJob);
    log.info("Extracted {} files from {}", extractedFiles.size(), zipFile);
  }

  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {
    currentProcessJob.getProcessingResults().add(
        ProcessResult.builder()
            .processName(UNZIP)
            .processStatus(SUCCESS)
            .build());
  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {
    currentProcessJob.getProcessingResults().add(
        ProcessResult.builder()
            .processName(UNZIP)
            .processStatus(FAILED)
            .failureReason(failureReason)
            .build());
  }

  private Path resolveExtractionDirectory(ProcessJob processJob) {
    return processJob.getPayloadLocation().toPath().getParent().resolve(
        processJob.getPayloadLocation().getName() + "_extracted");
  }

  private List<ProcessJob> getExtractedFiles(Path destinationDirectory, ProcessJob processJob) {
    return Arrays.stream(destinationDirectory.toFile().listFiles()).map(
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
