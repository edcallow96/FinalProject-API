package com.finalproject.backend;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;

@ConfigurationProperties
@Getter
@Setter
public class ApplicationProperties {
  private String deepSecureApiKey;
  private URI deepSecureEndpoint;

  private Path downloadDirectory;
  private String awsRegion;

  private URI metaDefenderEndpoint;
  private String metaDefenderApiKey;
  private int metaDefenderPollingDelay;
  private int metaDefenderPollingTimeout;

  private String treatedBucketName;

  private int selfSignedUrlExpirationDays;

  private String notificationSenderAddress;
}
