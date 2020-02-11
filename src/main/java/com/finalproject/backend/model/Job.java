package com.finalproject.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.tika.mime.MediaType;

import java.io.File;

@Builder
@Getter
@Setter
@ToString
public class Job {
  private String jobId;
  private MediaType contentType;
  private File payloadLocation;
  private String sourceBucket;
  private String sourceKey;
}
