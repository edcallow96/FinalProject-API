package com.finalproject.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class ProcessResult {

  private ProcessStatus processStatus;
  private String failureReason;
  private ProcessName processName;
}
