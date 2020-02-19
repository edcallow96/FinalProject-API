package com.finalproject.backend;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@ConfigurationProperties
@Getter
@Setter
@Component
public class ApplicationProperties {
  private String deepSecureApiKey;
  private Path downloadDirectory;
  private String awsRegion;
}
