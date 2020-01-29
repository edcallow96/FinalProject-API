package com.finalproject.backend.fileidentification;

import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.FILE_TYPE_HEADER;

@Component
@Slf4j
public class FileIdentificationProcessor implements Processor {

  private final FileIdentifier fileIdentifier;

  public FileIdentificationProcessor(final FileIdentifier fileIdentifier) {
    this.fileIdentifier = fileIdentifier;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    S3Object s3Object = exchange.getIn().getBody(S3Object.class);
    log.info("Determining the file type of {}", s3Object.getKey());
    MediaType fileType = fileIdentifier.identifyFile(s3Object.getObjectContent());
    log.info("The file type of {} is {}", s3Object.getKey(), fileType);
    exchange.getIn().setHeader(FILE_TYPE_HEADER, fileType);
  }
}
