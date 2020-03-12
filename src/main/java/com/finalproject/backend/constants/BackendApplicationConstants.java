package com.finalproject.backend.constants;

public class BackendApplicationConstants {
  private BackendApplicationConstants() {
  }

  // Camel Endpoints
  public static final String ENTRY_POINT_ROUTE = "direct:entryPoint";
  public static final String FILE_IDENTIFICATION_ROUTE = "direct:fileIdentificationRoute";
  public static final String THREAT_REMOVAL_ROUTE = "direct:threatRemovalRoute";
  public static final String ANTI_VIRUS_SCANNING_ROUTE = "direct:avScanningRoute";
  public static final String SEND_FAILURE_NOTIFICATION = "direct:sendFailureNotification";
  public static final String SEND_SUCCESS_NOTIFICATION = "direct:sendSuccessNotification";
  public static final String PROCESS_JOB = "direct:processJob";

  // Exchange Headers
  public static final String AMAZON_REQUEST_ID = "amazonRequestId";
  public static final String EXTRACTED_FILE_RESULTS = "extractedFileResults";

  //AWS Metadata header
  public static final String AMZ_METADATA_USER_ID = "userId";

  public static final int BYTES_PER_MEGABYTE = 1048576;
}
