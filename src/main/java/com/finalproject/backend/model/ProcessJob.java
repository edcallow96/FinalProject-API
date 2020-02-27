package com.finalproject.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class ProcessJob {
  private String jobId;
  private MediaType contentType;
  private File payloadLocation;
  private String sourceBucket;
  private String sourceKey;
  @Builder.Default
  private List<ProcessResult> processingResults = new ArrayList<>();
  private User user;

  private String treatedBucketKey;
  private String originalFileHash;
  private long originalFileSize;
}
