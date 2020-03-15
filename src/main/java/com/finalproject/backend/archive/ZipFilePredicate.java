package com.finalproject.backend.archive;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;

import static org.apache.tika.mime.MediaType.APPLICATION_ZIP;

@Component
public class ZipFilePredicate implements Predicate {

  @Override
  public boolean matches(Exchange exchange) {
    MediaType contentType = exchange.getIn().getBody(ProcessJob.class).getContentType();
    return contentType != null && contentType.equals(APPLICATION_ZIP);
  }
}
