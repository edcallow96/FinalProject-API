package com.finalproject.backend.notification;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_FAILURE_NOTIFICATION;
import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_SUCCESS_NOTIFICATION;

@Component
public class NotificationRoute extends RouteBuilder {
  @Override
  public void configure() {
    from(SEND_SUCCESS_NOTIFICATION)
        .log("Success notification")
        .end();

    from(SEND_FAILURE_NOTIFICATION)
        .log("Failure notification")
        .end();
  }
}
