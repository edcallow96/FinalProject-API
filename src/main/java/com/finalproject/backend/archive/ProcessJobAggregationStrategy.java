package com.finalproject.backend.archive;

import com.finalproject.backend.model.ProcessJob;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessJobAggregationStrategy implements AggregationStrategy {

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    List<ProcessJob> aggregatedJobs = oldExchange == null ? new ArrayList<>() : oldExchange.getIn().getBody(List.class);
    aggregatedJobs.add(newExchange.getIn().getBody(ProcessJob.class));
    newExchange.getIn().setBody(aggregatedJobs);
    return newExchange;
  }
}
