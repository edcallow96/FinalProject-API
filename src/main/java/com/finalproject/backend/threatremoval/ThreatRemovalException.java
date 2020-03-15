package com.finalproject.backend.threatremoval;

public class ThreatRemovalException extends Exception {
  public ThreatRemovalException(String message) {
    super(message);
  }

  public ThreatRemovalException(String message, Exception exception) {
    super(message, exception);
  }
}
