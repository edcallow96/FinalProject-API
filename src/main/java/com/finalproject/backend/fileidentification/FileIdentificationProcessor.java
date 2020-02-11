package com.finalproject.backend.fileidentification;

import com.finalproject.backend.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileIdentificationProcessor implements Processor {

  private final FileIdentifier fileIdentifier;

  public FileIdentificationProcessor(final FileIdentifier fileIdentifier) {
    this.fileIdentifier = fileIdentifier;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Job job = exchange.getIn().getBody(Job.class);
    log.info("Determining the file type of {}", job.getPayloadLocation());
    MediaType fileType = fileIdentifier.identifyFile(job.getPayloadLocation());
    log.info("The file type of {} is {}", job.getPayloadLocation(), fileType);
    job.setContentType(fileType);
  }
}
