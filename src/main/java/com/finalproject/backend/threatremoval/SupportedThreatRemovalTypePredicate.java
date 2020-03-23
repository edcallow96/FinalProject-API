package com.finalproject.backend.threatremoval;

import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.finalproject.backend.constants.BackendApplicationConstants.BYTES_PER_MEGABYTE;

@Component
@Slf4j
public class SupportedThreatRemovalTypePredicate implements Predicate {

  private final ApplicationProperties applicationProperties;

  private List<String> types = Arrays.asList(
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/vnd.ms-powerpoint.presentation.macroenabled.12",
      "application/vnd.ms-excel.sheet.macroenabled.12",
      "application/vnd.ms-word.document.macroenabled.12",
      "image/gif",
      "image/bmp",
      "image/jpeg",
      "image/jpg",
      "image/jp2",
      "image/png",
      "image/x-ms-bmp");

  public SupportedThreatRemovalTypePredicate(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public boolean matches(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    boolean fileSizeSupported = processJob.getOriginalFileSize() <= applicationProperties.getMaxThreatRemovalFileSize() * BYTES_PER_MEGABYTE;
    boolean contentTypeSupported = processJob.getContentType().toString() != null && types.contains(processJob.getContentType().toString());
    return contentTypeSupported && fileSizeSupported;
  }
}
