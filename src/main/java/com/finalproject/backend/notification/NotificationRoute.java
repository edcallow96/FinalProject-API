package com.finalproject.backend.notification;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_FAILURE_NOTIFICATION_ROUTE;
import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_SUCCESS_NOTIFICATION_ROUTE;

@Component
public class NotificationRoute extends RouteBuilder {

  private final UploadTreatedFileProcessor uploadTreatedFileProcessor;
  private final SuccessNotificationProcessor successNotificationProcessor;
  private final FailureNotificationProcessor failureNotificationProcessor;

  public NotificationRoute(final UploadTreatedFileProcessor uploadTreatedFileProcessor,
                           final SuccessNotificationProcessor successNotificationProcessor,
                           final FailureNotificationProcessor failureNotificationProcessor) {
    this.uploadTreatedFileProcessor = uploadTreatedFileProcessor;
    this.successNotificationProcessor = successNotificationProcessor;
    this.failureNotificationProcessor = failureNotificationProcessor;
  }

  @Override
  public void configure() {
    from(SEND_SUCCESS_NOTIFICATION_ROUTE)
        .routeId("jobSuccess")
        .log("Success notification")
        .process(uploadTreatedFileProcessor)
        .process(successNotificationProcessor)
        .end();

    from(SEND_FAILURE_NOTIFICATION_ROUTE)
        .routeId("jobFailed")
        .log("Failure notification")
        .process(failureNotificationProcessor)
        .end();
  }
}
