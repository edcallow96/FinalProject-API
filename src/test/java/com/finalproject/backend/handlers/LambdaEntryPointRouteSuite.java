package com.finalproject.backend.handlers;

import com.finalproject.backend.archive.ZipFilePredicate;
import com.finalproject.backend.common.BaseRouteTest;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.finalproject.backend.constants.BackendApplicationConstants.*;
import static org.apache.tika.mime.MediaType.OCTET_STREAM;
import static org.mockito.Mockito.*;

@MockEndpoints(ENTRY_POINT_ROUTE + "|" + PROCESS_JOB_PIPELINE_ROUTE + "|" + JOB_COMPLETION_ROUTE)
public class LambdaEntryPointRouteSuite extends BaseRouteTest {

  @EndpointInject("mock:" + ENTRY_POINT_ROUTE)
  private MockEndpoint mockEntryPointEndpoint;

  @EndpointInject("mock:" + FILE_IDENTIFICATION_ROUTE)
  private MockEndpoint mockFileIdentificationEndpoint;

  @EndpointInject("mock:" + THREAT_REMOVAL_ROUTE)
  private MockEndpoint mockThreatRemovalEndpoint;

  @EndpointInject("mock:" + ANTI_VIRUS_SCANNING_ROUTE)
  private MockEndpoint mockAntiVirusEndpoint;

  @EndpointInject("mock:" + SEND_SUCCESS_NOTIFICATION_ROUTE)
  private MockEndpoint mockSendSuccessNotificationEndpoint;

  @EndpointInject("mock:" + SEND_FAILURE_NOTIFICATION_ROUTE)
  private MockEndpoint mockSendFailureNotificationEndpoint;

  @EndpointInject("mock:" + JOB_COMPLETION_ROUTE)
  private MockEndpoint mockJobCompletionEndpoint;

  @EndpointInject("mock:" + PROCESS_JOB_PIPELINE_ROUTE)
  private MockEndpoint mockProcessJobPipelineEndpoint;

  @MockBean
  private PrepareJobProcessor prepareJobProcessor;

  @MockBean
  private ZipFilePredicate zipFilePredicate;

  @MockBean
  private JobFailedPredicate jobFailedPredicate;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    when(jobFailedPredicate.matches(exchange)).thenReturn(false);
    mockFileIdentificationEndpoint.whenAnyExchangeReceived(receivedExchnage -> receivedExchnage.getIn().getBody(ProcessJob.class).setContentType(OCTET_STREAM));
  }

  @Test
  public void entryPointRoutePreparesNormalJobAndSendsToProcessJobRoute() throws Exception {
    when(zipFilePredicate.matches(exchange)).thenReturn(false);
    mockFileIdentificationEndpoint.setExpectedCount(1);
    mockProcessJobPipelineEndpoint.setExpectedCount(1);
    mockUnzipFileEndpoint.setExpectedCount(0);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    verify(prepareJobProcessor).process(exchange);
    mockFileIdentificationEndpoint.assertIsSatisfied();
    verify(zipFilePredicate).matches(exchange);
    mockUnzipFileEndpoint.assertIsSatisfied();
    mockProcessJobPipelineEndpoint.assertIsSatisfied();
  }

  @Test
  public void entryPointRoutePreparesZipJobAndSendsToUnzipFileRoute() throws Exception {
    when(zipFilePredicate.matches(exchange)).thenReturn(true);

    mockFileIdentificationEndpoint.setExpectedCount(1);
    mockProcessJobPipelineEndpoint.setExpectedCount(0);
    mockUnzipFileEndpoint.setExpectedCount(1);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    verify(prepareJobProcessor).process(exchange);
    mockFileIdentificationEndpoint.assertIsSatisfied();
    verify(zipFilePredicate).matches(exchange);
    mockUnzipFileEndpoint.assertIsSatisfied();
    mockProcessJobPipelineEndpoint.assertIsSatisfied();
  }

  @Test
  public void processJobRouteSendsJobToEachProcessingEndpoint() throws InterruptedException {
    mockFileIdentificationEndpoint.setExpectedCount(1);
    mockThreatRemovalEndpoint.setExpectedCount(1);
    mockAntiVirusEndpoint.setExpectedCount(1);

    templateProducer.send(PROCESS_JOB_PIPELINE_ROUTE, exchange);

    mockFileIdentificationEndpoint.assertIsSatisfied();
    mockThreatRemovalEndpoint.assertIsSatisfied();
    mockAntiVirusEndpoint.assertIsSatisfied();

    verify(jobFailedPredicate, times(3)).matches(exchange);
  }

  @Test
  public void processJobRouteDoesNotRouteToFileIdentificationRouteWhenContentTypeIsAlreadySet() throws InterruptedException {
    exchange.getIn().getBody(ProcessJob.class).setContentType(OCTET_STREAM);

    mockFileIdentificationEndpoint.setExpectedCount(0);
    mockThreatRemovalEndpoint.setExpectedCount(1);
    mockAntiVirusEndpoint.setExpectedCount(1);

    templateProducer.send(PROCESS_JOB_PIPELINE_ROUTE, exchange);

    mockFileIdentificationEndpoint.assertIsSatisfied();
    mockThreatRemovalEndpoint.assertIsSatisfied();
    mockAntiVirusEndpoint.assertIsSatisfied();

    verify(jobFailedPredicate, times(3)).matches(exchange);
  }

  @Test
  public void processJobRouteDoesNotRouteJobsToProcessingEndpointsWhenJobHasFailed() throws InterruptedException {
    when(jobFailedPredicate.matches(exchange)).thenReturn(true);

    mockFileIdentificationEndpoint.setExpectedCount(0);
    mockThreatRemovalEndpoint.setExpectedCount(0);
    mockAntiVirusEndpoint.setExpectedCount(0);

    templateProducer.send(PROCESS_JOB_PIPELINE_ROUTE, exchange);

    mockFileIdentificationEndpoint.assertIsSatisfied();
    mockThreatRemovalEndpoint.assertIsSatisfied();
    mockAntiVirusEndpoint.assertIsSatisfied();

    verify(jobFailedPredicate, times(3)).matches(exchange);
  }

  @Test
  public void processJobRouteDoesNotRouteJobsToFurtherProcessingEndpointsWhenJobHasFailed() throws InterruptedException {
    when(jobFailedPredicate.matches(exchange)).thenReturn(false).thenReturn(true);

    mockFileIdentificationEndpoint.setExpectedCount(1);
    mockThreatRemovalEndpoint.setExpectedCount(0);
    mockAntiVirusEndpoint.setExpectedCount(0);

    templateProducer.send(PROCESS_JOB_PIPELINE_ROUTE, exchange);

    mockFileIdentificationEndpoint.assertIsSatisfied();
    mockThreatRemovalEndpoint.assertIsSatisfied();
    mockAntiVirusEndpoint.assertIsSatisfied();

    verify(jobFailedPredicate, times(3)).matches(exchange);
  }

  @Test
  public void jobCompletionRouteSendsJobToSuccessNotificationRouteWhenJobHasNotFailed() throws InterruptedException {
    mockSendSuccessNotificationEndpoint.setExpectedCount(1);

    templateProducer.send(JOB_COMPLETION_ROUTE, exchange);

    mockSendSuccessNotificationEndpoint.assertIsSatisfied();
    verify(jobFailedPredicate).matches(exchange);
  }

  @Test
  public void jobCompletionRouteSendsJobToSuccessNotificationRouteWhenJobHasFailed() throws InterruptedException {
    when(jobFailedPredicate.matches(exchange)).thenReturn(true);
    mockSendFailureNotificationEndpoint.setExpectedCount(1);

    templateProducer.send(JOB_COMPLETION_ROUTE, exchange);

    mockSendFailureNotificationEndpoint.assertIsSatisfied();
    verify(jobFailedPredicate).matches(exchange);
  }

  @Test
  public void entryPointRouteSendsJobToJobCompletionRouteAfterProcessing() throws InterruptedException {
    mockJobCompletionEndpoint.setExpectedCount(1);

    templateProducer.send(ENTRY_POINT_ROUTE, exchange);

    mockJobCompletionEndpoint.assertIsSatisfied();
  }


}