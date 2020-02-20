package com.finalproject.backend.handlers;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.model.ProcessStatus.FAILED;

@Component
public class JobFailedPredicate implements Predicate {
  @Override
  public boolean matches(Exchange exchange) {
    return exchange
        .getIn()
        .getBody(ProcessJob.class)
        .getProcessingResults()
        .stream()
        .anyMatch(it -> it.getProcessStatus() == FAILED);
  }
}
