package com.finalproject.backend.antivirus;

import com.finalproject.backend.common.PayloadProcessor;
import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
public class AntiVirusProcessor extends PayloadProcessor {
  @Override
  protected void succeedCurrentJob(ProcessJob currentProcessJob) {

  }

  @Override
  protected void failCurrentJob(ProcessJob currentProcessJob, String failureReason) {

  }

  @Override
  protected void processCurrentJob(ProcessJob currentProcessJob) {

  }

  @Override
  public void process(Exchange exchange) throws Exception {

  }
}
