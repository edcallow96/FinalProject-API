package com.finalproject.backend.antivirus;

public class AntiVirusException extends Exception {
  public AntiVirusException(String message) {
    super(message);
  }

  public AntiVirusException(String message, Exception exception) {
    super(message, exception);
  }
}
