package com.finalproject.backend.threatremoval;

import com.amazonaws.util.IOUtils;
import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.model.ProcessJob;
import com.finalproject.backend.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import static com.finalproject.backend.model.ProcessName.THREAT_REMOVAL;
import static com.finalproject.backend.model.ProcessStatus.FAILED;
import static com.finalproject.backend.model.ProcessStatus.SUCCESS;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class ThreatRemovalProcessor extends PayloadProcessor {

  private final ApplicationProperties applicationProperties;

  public ThreatRemovalProcessor(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void process(Exchange exchange) {
    processCurrentJob(exchange.getIn().getBody(ProcessJob.class));
  }

  @Override
  protected void processCurrentJob(ProcessJob currentProcessJob) {
    try {
      log.info("Attempting to sanitise {} with file hash {}", currentProcessJob.getPayloadLocation(), md5Hash(currentProcessJob.getPayloadLocation()));
      HttpEntity<FileSystemResource> httpEntity = buildHttpEntity(currentProcessJob);
      uploadFileForSanitisaion(httpEntity, currentProcessJob.getPayloadLocation());
      succeedCurrentJob(currentProcessJob);
      log.info("Sanitisation of file {} was successful. New file hash is {}",
          currentProcessJob.getPayloadLocation(), md5Hash(currentProcessJob.getPayloadLocation()));
    } catch (Exception exception) {
      failCurrentJob(currentProcessJob, exception.getMessage());
    }
  }

  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {
    log.error("Succeeding threat removal for job {}", currentProcessJob.getJobId());
    currentProcessJob.getProcessingResults().add(ProcessResult.builder()
        .processName(THREAT_REMOVAL)
        .processStatus(SUCCESS)
        .build());
  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {
    log.error("Failing treat removal for job {} due to {}", currentProcessJob.getJobId(), failureReason);
    currentProcessJob.getProcessingResults().add(ProcessResult.builder()
        .processName(THREAT_REMOVAL)
        .processStatus(FAILED)
        .failureReason(failureReason)
        .build());
  }

  private HttpEntity<FileSystemResource> buildHttpEntity(ProcessJob processJob) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", processJob.getContentType().toString() + "," + APPLICATION_JSON);
    headers.add("Content-Type", processJob.getContentType().toString());
    headers.add("x-api-key", applicationProperties.getDeepSecureApiKey());
    headers.add("X-Accept-Preview-Mode-For-Content-Types", processJob.getContentType().toString());
    return new HttpEntity<>(new FileSystemResource(processJob.getPayloadLocation()), headers);
  }

  private void uploadFileForSanitisaion(HttpEntity<FileSystemResource> httpEntity, File payloadLocation) throws Exception {
    ResponseEntity<Resource> responseEntity =
        new RestTemplate().exchange(URI.create(String.format("%s/upload", applicationProperties.getDeepSecureEndpoint())),
            HttpMethod.POST, httpEntity, Resource.class);
    if (responseEntity.getStatusCode() != HttpStatus.OK) {
      throw new ThreatRemovalException(
          String.format("File sanitisation failed reason: %s", IOUtils.toString(responseEntity.getBody().getInputStream())));
    }
    FileUtils.copyInputStreamToFile(responseEntity.getBody().getInputStream(), payloadLocation);
  }

  private String md5Hash(File payloadLocaton) throws Exception {
    return DigestUtils.md5Hex(new FileInputStream(payloadLocaton)).toUpperCase();
  }
}
