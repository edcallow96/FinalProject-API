package com.finalproject.backend.common;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Processor;

public abstract class PayloadProcessor implements Processor {
  abstract protected void succeedCurrentJob(ProcessJob currentProcessJob);

  abstract protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason);

  abstract protected void processCurrentJob(ProcessJob currentProcessJob);
}
