package com.finalproject.backend.fileidentification;

import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import static com.finalproject.backend.model.ProcessName.FILE_IDENTIFICATION;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;

@Component
@Slf4j
public class FileIdentificationProcessor extends PayloadProcessor {

  private final FileIdentifier fileIdentifier;

  public FileIdentificationProcessor(final FileIdentifier fileIdentifier) {
    this.fileIdentifier = fileIdentifier;
  }

  @Override
  public void process(Exchange exchange) {
    processCurrentJob(exchange.getIn().getBody(ProcessJob.class));
  }

  @Override
  protected void processCurrentJob(ProcessJob currentProcessJob) {
    try {
      currentProcessJob.setContentType(identifyFileType(currentProcessJob.getPayloadLocation()));
      succeedCurrentJob(currentProcessJob);
    } catch (Exception exception) {
      failCurrentJob(currentProcessJob, exception.getMessage());
    }
  }

  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {
    currentProcessJob.getProcessingResults().add(ProcessResult.builder()
        .processName(FILE_IDENTIFICATION)
        .processStatus(SUCCESS)
        .build());
  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {
    currentProcessJob.getProcessingResults().add(ProcessResult.builder()
        .processName(FILE_IDENTIFICATION)
        .processStatus(FAILED)
        .failureReason(failureReason)
        .build());
  }

  private MediaType identifyFileType(File payloadLocation) throws IOException {
    log.info("Determining the file type of {}", payloadLocation);
    MediaType fileType = fileIdentifier.identifyFile(payloadLocation);
    log.info("The file type of {} is {}", payloadLocation, fileType);
    return fileType;
  }


}
