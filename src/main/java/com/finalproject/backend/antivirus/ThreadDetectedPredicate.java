package com.finalproject.backend.antivirus;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

@Component
public class ThreadDetectedPredicate implements Predicate {
  @Override
  public boolean matches(Exchange exchange) {
    return false;
  }
}
