package com.finalproject.backend.notification;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static j2html.TagCreator.*;
import static java.lang.String.format;

@Slf4j
@Component
public class FailureNotificationProcessor extends BaseNotificationProcessor {

  public FailureNotificationProcessor(final AmazonSimpleEmailService amazonSimpleEmailService,
                                      final ApplicationProperties applicationProperties) {
    super(amazonSimpleEmailService, applicationProperties);
  }

  @Override
  public void process(Exchange exchange) {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    log.info("Sending failure notification for Job {}", processJob.getJobId());
    try {
      sendNotification("Your file has failed processing!",
          generateHtmlBody(processJob), processJob.getUser().getEmailAddress());
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private String generateHtmlBody(ProcessJob processJob) {
    ProcessResult failedProcess = processJob.getProcessingResults().stream().filter(it -> it.getProcessStatus() == FAILED).findFirst().get();
    return body(
        h1(format("Hello, %s! Here is the link to your processed file:", processJob.getUser().getFirstName())),
        h2(format("File name: %s", processJob.getPayloadLocation().getName())),
        h3(format("Original file hash: %s", processJob.getOriginalFileHash())),
        h3(format("Original file size: %s", processJob.getOriginalFileSize())),
        br(),
        p(format("Your file failed the %s process because %s", failedProcess.getProcessName(), failedProcess.getFailureReason()))
    ).render();
  }
}
