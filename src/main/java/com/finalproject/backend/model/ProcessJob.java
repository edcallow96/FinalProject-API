package com.finalproject.backend.model;

import lombok.*;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ProcessJob implements Serializable {
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
