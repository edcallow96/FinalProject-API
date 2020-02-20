package com.finalproject.backend.threatremoval;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SupportedThreatRemovalTypePredicate implements Predicate {

  private List<String> types = Arrays.asList(
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
      "application/vnd.ms-excel.sheet.macroEnabled.12",
      "application/vnd.ms-word.document.macroEnabled.12",
      "image/gif",
      "image/bmp",
      "image/jpeg",
      "image/jpg",
      "image/jp2",
      "image/png",
      "image/x-ms-bmp");

  @Override
  public boolean matches(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    return processJob.getContentType() != null && types.contains(processJob.getContentType().toString());
  }
}
