package com.finalproject.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
public class ProcessResult implements Serializable {

  private ProcessStatus processStatus;
  private String failureReason;
  private ProcessName processName;
}
