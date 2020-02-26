package com.finalproject.backend.antivirus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AntiVirusResponse {
  @JsonProperty("process_info")
  private ProcessInfo processInfo;

  @JsonProperty("scan_results")
  private ScanResults scanResults;

  @Getter
  @Setter
  @NoArgsConstructor
  @ToString
  public static class ProcessInfo {
    @JsonProperty("progress_percentage")
    private int progressPercentage;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @ToString
  public static class ScanResults {
    @JsonProperty("scan_all_result_i")
    private int scanResultCode;

    @JsonProperty("scan_details")
    private Map<String, ScanDetail> scanDetails;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @ToString
  public static class ScanDetail {
    @JsonProperty("threat_found")
    private String threatFound;
  }
}
