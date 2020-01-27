package com.finalproject.backend.antivirus;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

@Component
public class VirusDetectedPredicate implements Predicate {
  @Override
  public boolean matches(Exchange exchange) {
    return false;
  }
}
