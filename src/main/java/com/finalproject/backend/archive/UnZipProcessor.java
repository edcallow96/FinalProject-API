package com.finalproject.backend.archive;

import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.SerializationUtils;
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
      log.info("Extracted {} files from {}", extractedFiles.size(), zipFile.getFile());
      exchange.getIn().setBody(extractedFiles);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<ProcessJob> getExtractedFiles(Path destinationDirectory, ProcessJob processJob) {
    return asList(destinationDirectory.toFile().listFiles()).stream().map(extractedFile -> {
      log.info("Building processJob for extracted file {}", extractedFile);
      ProcessJob extractedProcessJob = SerializationUtils.clone(processJob);
      extractedProcessJob.setPayloadLocation(extractedFile);
      return extractedProcessJob;
    }).collect(toList());
  }
}
