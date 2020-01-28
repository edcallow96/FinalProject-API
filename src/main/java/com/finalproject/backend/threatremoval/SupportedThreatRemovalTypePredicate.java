package com.finalproject.backend.threatremoval;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

@Component
public class SupportedThreatRemovalTypePredicate implements Predicate {
  @Override
  public boolean matches(Exchange exchange) {
    return false;
  }
}
