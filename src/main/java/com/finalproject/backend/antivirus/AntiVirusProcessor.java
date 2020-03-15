package com.finalproject.backend.antivirus;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.finalproject.backend.model.ProcessName.ANTI_VIRUS_SCAN;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
public class AntiVirusProcessor extends PayloadProcessor {

  private final ApplicationProperties applicationProperties;

  public AntiVirusProcessor(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void process(Exchange exchange) {
    processCurrentJob(exchange.getIn().getBody(ProcessJob.class));
  }

  @Override
  protected void processCurrentJob(ProcessJob currentProcessJob) {
    try {
      String dataId = uploadFileForScan(currentProcessJob.getPayloadLocation());
      log.info("File uploaded for AV scanning with dataId {}", dataId);
      AntiVirusResponse antiVirusResponse = waitForScanToComplete(dataId);
      processAntiVirusResponse(antiVirusResponse, currentProcessJob.getPayloadLocation());
      succeedCurrentJob(currentProcessJob);
    } catch (Exception exception) {
      log.error("AntiVirus processing failed", exception);
      failCurrentJob(currentProcessJob, exception.getMessage());
    }
  }

  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {
    log.info("Succeeding anti virus scan for job {}", currentProcessJob.getJobId());
    currentProcessJob.getProcessingResults().add(ProcessResult.builder()
        .processName(ANTI_VIRUS_SCAN)
        .processStatus(SUCCESS)
        .build());
  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {
    log.error("Failing anti virus scanning for job {} due to {}", currentProcessJob.getJobId(), failureReason);
    currentProcessJob.getProcessingResults().add(ProcessResult.builder()
        .processName(ANTI_VIRUS_SCAN)
        .processStatus(FAILED)
        .failureReason(failureReason)
        .build());
  }

  private String uploadFileForScan(File payloadLocation) {
    HttpEntity<FileSystemResource> httpEntity = buildHttpEntity(payloadLocation);
    ResponseEntity<ObjectNode> scanResponse =
        new RestTemplate().exchange(applicationProperties.getMetaDefenderEndpoint(), HttpMethod.POST, httpEntity, ObjectNode.class);
    return scanResponse.getBody().get("data_id").asText();
  }

  private AntiVirusResponse waitForScanToComplete(String dataId) throws Exception {
    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    Future<AntiVirusResponse> result = service.submit(() -> {
      AntiVirusResponse antiVirusResponse = null;
      int scanProgress = 0;
      while (scanProgress != 100) {
        antiVirusResponse = getCurrentScanStatus(dataId);
        if (antiVirusResponse != null && antiVirusResponse.getProcessInfo() != null) {
          scanProgress = antiVirusResponse.getProcessInfo().getProgressPercentage();
        }
        Thread.sleep(applicationProperties.getMetaDefenderPollingDelay());
      }
      return antiVirusResponse;
    });
    return result.get(applicationProperties.getMetaDefenderPollingTimeout(), TimeUnit.MILLISECONDS);
  }

  private AntiVirusResponse getCurrentScanStatus(String dataId) {
    ResponseEntity<AntiVirusResponse> scanResultResponse =
        new RestTemplate().exchange(
            String.format("%s/%s", applicationProperties.getMetaDefenderEndpoint(), dataId), HttpMethod.GET, buildHttpEntity(), AntiVirusResponse.class);
    return scanResultResponse.getBody();
  }

  private void processAntiVirusResponse(AntiVirusResponse antiVirusResponse, File payloadLocation) throws AntiVirusException {
    log.info("Scan results: {}", antiVirusResponse);
    if (antiVirusResponse.getScanResults().getScanResultCode() != 0) {
      String threatFound = antiVirusResponse.getScanResults().getScanDetails().values().stream().filter(it -> isNotBlank(it.getThreatFound())).findFirst().get().getThreatFound();
      throw new AntiVirusException(String.format("%s is infected. Threat found %s.", payloadLocation.getName(), threatFound));
    }
  }

  private HttpEntity<FileSystemResource> buildHttpEntity(File payloadLocation) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("apiKey", applicationProperties.getMetaDefenderApiKey());
    headers.add("filename", payloadLocation.getName());
    headers.add("content-type", APPLICATION_OCTET_STREAM);
    return new HttpEntity<>(new FileSystemResource(payloadLocation), headers);
  }

  private HttpEntity<Void> buildHttpEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("apiKey", applicationProperties.getMetaDefenderApiKey());
    return new HttpEntity<>(headers);
  }
}
