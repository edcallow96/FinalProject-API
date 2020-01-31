package com.finalproject.backend.fileidentification;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

import java.io.File;

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
    File downloadedFile = exchange.getIn().getBody(File.class);
    log.info("Determining the file type of {}", downloadedFile);
    MediaType fileType = fileIdentifier.identifyFile(downloadedFile);
    log.info("The file type of {} is {}", downloadedFile, fileType);
    exchange.getIn().setHeader(FILE_TYPE_HEADER, fileType);
  }
}
