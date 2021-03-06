package com.finalproject.backend.notification;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import j2html.tags.DomContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static j2html.TagCreator.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;

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
      log.error("Sending failure notification failed.", exception);
    }
  }

  private String generateHtmlBody(ProcessJob processJob) throws IOException {
    ProcessResult failedProcess = processJob.getProcessingResults().stream().filter(it -> it.getProcessStatus() == FAILED).findFirst().orElse(null);
    List<DomContent> bodyContents = new ArrayList<>(asList(
        getCssStyling(),
        h1(format("Hello %s, your file has failed processing! Details below:", processJob.getUser().getFirstName())),
        h2(format("Job Id: %s", processJob.getJobId()))));

    bodyContents.addAll(getFileInfoSection(processJob));

    bodyContents.add(getProcessingResultsTable(processJob));

    String failedProcessName = failedProcess != null ? failedProcess.getProcessName().name() : "";
    String failureReason = failedProcess != null ? failedProcess.getFailureReason() : "an unexpected failure occurred";
    bodyContents.add(p(format("Your file failed processing %s because %s. Please contact the support team referencing your JobId.",
        failedProcessName, failureReason)));

    return body(bodyContents.toArray(new DomContent[0])).render();
  }
}
