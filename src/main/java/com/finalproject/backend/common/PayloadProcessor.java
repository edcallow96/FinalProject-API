package com.finalproject.backend.common;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Processor;

public abstract class PayloadProcessor implements Processor {
  protected abstract void processCurrentJob(ProcessJob currentProcessJob);

  protected abstract void succeedCurrentJob(ProcessJob currentProcessJob);

  protected abstract void failCurrentJob(ProcessJob currentProcessJob, String failureReason);
}
