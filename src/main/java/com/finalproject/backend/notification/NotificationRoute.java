package com.finalproject.backend.notification;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_FAILURE_NOTIFICATION;
import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_SUCCESS_NOTIFICATION;

@Component
public class NotificationRoute extends RouteBuilder {

  private final UploadTreatedFileProcessor uploadTreatedFileProcessor;
  private final SuccessNotificationProcessor successNotificationProcessor;

  public NotificationRoute(final UploadTreatedFileProcessor uploadTreatedFileProcessor,
                           final SuccessNotificationProcessor successNotificationProcessor) {
    this.uploadTreatedFileProcessor = uploadTreatedFileProcessor;
    this.successNotificationProcessor = successNotificationProcessor;
  }

  @Override
  public void configure() {
    from(SEND_SUCCESS_NOTIFICATION)
        .routeId("jobSuccess")
        .log("Success notification")
        .process(uploadTreatedFileProcessor)
        .process(successNotificationProcessor)
        .end();

    from(SEND_FAILURE_NOTIFICATION)
        .log("Failure notification")
        .end();
  }
}
