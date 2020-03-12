package com.finalproject.backend.archive;

import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessName;
import com.finalproject.backend.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.finalproject.backend.constants.BackendApplicationConstants.EXTRACTED_FILE_RESULTS;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@Component
@Slf4j
public class ZipProcessor implements Processor {

  private final ApplicationProperties applicationProperties;

  public ZipProcessor(ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    List<ProcessJob> processedFiles = exchange.getIn().getBody(List.class);
    exchange.getIn().setHeader(EXTRACTED_FILE_RESULTS, processedFiles);
    File zippedFile = zipProcessedFiles(processedFiles);
    ProcessJob processJob = aggregateProcessingResults(processedFiles);
    processJob.setPayloadLocation(zippedFile);
    exchange.getIn().setBody(processJob);
  }

  private File zipProcessedFiles(List<ProcessJob> processedFiles) throws ZipException {
    String originalZipFile = processedFiles.get(0).getSourceKey();
    Path zipFileLocation = applicationProperties.getDownloadDirectory().resolve(format("%s/%s", randomAlphabetic(10), originalZipFile));
    log.info("Attempting to zip {} files into {}", processedFiles.size(), zipFileLocation);
    zipFileLocation.getParent().toFile().mkdir();
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
