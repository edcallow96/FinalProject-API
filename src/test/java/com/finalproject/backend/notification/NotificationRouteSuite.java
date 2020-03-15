package com.finalproject.backend.notification;

import com.finalproject.backend.common.BaseRouteTest;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_FAILURE_NOTIFICATION_ROUTE;
import static com.finalproject.backend.constants.BackendApplicationConstants.SEND_SUCCESS_NOTIFICATION_ROUTE;
import static org.mockito.Mockito.verify;

@MockEndpoints(SEND_SUCCESS_NOTIFICATION_ROUTE + "|" + SEND_FAILURE_NOTIFICATION_ROUTE)
public class NotificationRouteSuite extends BaseRouteTest {

  @MockBean
  private UploadTreatedFileProcessor uploadTreatedFileProcessor;

  @MockBean
  private SuccessNotificationProcessor successNotificationProcessor;

  @MockBean
  private FailureNotificationProcessor failureNotificationProcessor;

  @Test
  public void successNotificationRouteShouldUploadTreatedFileAndSendSuccessNotification() {
    templateProducer.send(SEND_SUCCESS_NOTIFICATION_ROUTE, exchange);

    verify(uploadTreatedFileProcessor).process(exchange);
    verify(successNotificationProcessor).process(exchange);
  }

  @Test
  public void failureNotificationRouteShouldSendFailureNotification() {
    templateProducer.send(SEND_FAILURE_NOTIFICATION_ROUTE, exchange);

    verify(failureNotificationProcessor).process(exchange);
  }
}