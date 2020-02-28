package com.finalproject.backend.notification;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.finalproject.backend.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Processor;

import static org.apache.commons.codec.CharEncoding.UTF_8;

@Slf4j
public abstract class BaseNotificationProcessor implements Processor {

  private final AmazonSimpleEmailService amazonSimpleEmailService;
  protected final ApplicationProperties applicationProperties;

  protected BaseNotificationProcessor(final AmazonSimpleEmailService amazonSimpleEmailService,
                                      final ApplicationProperties applicationProperties) {
    this.amazonSimpleEmailService = amazonSimpleEmailService;
    this.applicationProperties = applicationProperties;
  }

  protected void sendNotification(String subject, String htmlContent, String recipient) {
    log.info("Sending success notification to {}", recipient);
    SendEmailRequest request = new SendEmailRequest()
        .withDestination(
            new Destination().withToAddresses(recipient))
        .withMessage(new Message()
            .withBody(new Body()
                .withHtml(new Content()
                    .withCharset(UTF_8).withData(htmlContent)))
            .withSubject(new Content()
                .withCharset(UTF_8).withData(subject)))
        .withSource(applicationProperties.getNotificationSenderAddress());
    log.info("Request: {}", request);
    log.info("Email send: {}", amazonSimpleEmailService.sendEmail(request));
  }
}
