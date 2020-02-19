package com.finalproject.backend.fileidentification;

public class FileIdentificationException extends Exception {
  public FileIdentificationException(String message, Exception exception) {
    super(message, exception);
  }

  public FileIdentificationException(String message) {
    super(message);
  }
}
