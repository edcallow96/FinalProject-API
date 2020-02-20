package com.finalproject.backend.threatremoval;

import com.finalproject.backend.ApplicationProperties;
import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class ThreatRemovalProcessor extends PayloadProcessor {

  private final ApplicationProperties applicationProperties;

  public ThreatRemovalProcessor(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    ProcessJob processJob = exchange.getIn().getBody(ProcessJob.class);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", processJob.getContentType().toString() + "," + APPLICATION_JSON);
    headers.add("Content-Type", processJob.getContentType().toString());
    headers.add("x-api-key", applicationProperties.getDeepSecureApiKey());
    HttpEntity<FileSystemResource> httpEntity = new HttpEntity<>(new FileSystemResource(processJob.getPayloadLocation()), headers);
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<Resource> responseEntity =
        restTemplate.exchange(URI.create(String.format("%s/upload", applicationProperties.getDeepSecureEndpoint())),
            HttpMethod.POST, httpEntity, Resource.class);
    FileUtils.copyInputStreamToFile(responseEntity.getBody().getInputStream(), processJob.getPayloadLocation());
  }

  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {

  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {

  }

  @Override
  protected void processCurrentJob(ProcessJob currentProcessJob) {

  }
}
