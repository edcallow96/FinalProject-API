package com.finalproject.backend.archive;

import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.handlers.JobFailedPredicate;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessName;
import com.finalproject.backend.model.ProcessResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.finalproject.backend.constants.BackendApplicationConstants.EXTRACTED_FILE_RESULTS;
import static com.finalproject.backend.model.ProcessName.ZIP;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@Component
@Slf4j
public class ZipProcessor extends PayloadProcessor {

  private final ApplicationProperties applicationProperties;
  private final JobFailedPredicate jobFailedPredicate;

  public ZipProcessor(ApplicationProperties applicationProperties, JobFailedPredicate jobFailedPredicate) {
    this.applicationProperties = applicationProperties;
    this.jobFailedPredicate = jobFailedPredicate;
  }

  @Override
  public void process(Exchange exchange) {
    List<ProcessJob> processedFiles = exchange.getIn().getBody(List.class);
    try {
      exchange.getIn().setHeader(EXTRACTED_FILE_RESULTS, processedFiles);
      File zippedFile = zipProcessedFiles(processedFiles);
      ProcessJob processJob = aggregateProcessingResults(processedFiles);
      processJob.setPayloadLocation(zippedFile);
      exchange.getIn().setBody(processJob);
      if (!jobFailedPredicate.matches(exchange)) {
        succeedCurrentJob(processJob);
      }
    } catch (Exception e) {
      log.error("Zipping job {} failed", processedFiles.get(0).getJobId(), e);
      failCurrentJob(processedFiles.get(0), e.getMessage());
      exchange.getIn().setBody(processedFiles.get(0));
    }
  }

  @Override
  protected void processCurrentJob(ProcessJob currentProcessJob) {
    //Intentionally left empty
  }

  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {
    currentProcessJob.getProcessingResults().add(ProcessResult.builder().processName(ZIP).processStatus(SUCCESS).build());
  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {
    currentProcessJob.getProcessingResults().add(ProcessResult.builder().processName(ZIP).processStatus(FAILED).failureReason(failureReason).build());
  }

  @SneakyThrows
  private File zipProcessedFiles(List<ProcessJob> processedFiles) {
    String originalZipFile = processedFiles.get(0).getSourceKey();
    Path zipFileLocation = applicationProperties.getDownloadDirectory().resolve(format("%s/%s", randomAlphabetic(10), originalZipFile));
    log.info("Attempting to zip {} files into {}", processedFiles.size(), zipFileLocation);
    zipFileLocation.getParent().toFile().mkdirs();
    ZipFile zipFile = new ZipFile(zipFileLocation.toFile());
    for (ProcessJob file : processedFiles) {
      log.info("Zipping {}", file.getPayloadLocation());
      zipFile.addFile(file.getPayloadLocation());
    }
    return zipFile.getFile();
  }

  private ProcessJob aggregateProcessingResults(List<ProcessJob> processJobs) {
    log.info("Aggregating processing results");
    ProcessJob aggregatedProcessJob = SerializationUtils.clone(processJobs.get(0));

    log.info("Grouping results by process");
    Map<ProcessName, List<ProcessResult>> completeProcessingResults =
        processJobs.stream().flatMap(it -> it.getProcessingResults().stream()).collect(groupingBy(ProcessResult::getProcessName));

    List<ProcessResult> aggregatesProcessResults = new ArrayList<>();
    completeProcessingResults.forEach((key, value) -> {
      log.info("Aggregating {} results", key);
      ProcessResult failedResult = value.stream().filter(it -> it.getProcessStatus() == FAILED).findFirst().orElse(null);
      if (failedResult != null) {
        log.info("One of the files failed {}", key);
        aggregatesProcessResults.add(failedResult);
      } else {
        log.info("All files were successfully processed by {}", key);
        aggregatesProcessResults.add(value.get(0));
      }
    });
    aggregatedProcessJob.setProcessingResults(aggregatesProcessResults);
    return aggregatedProcessJob;
  }

}
