package org.codejudge.sb.models;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class LogAnalyzeRequest {

  @NotNull(message = "Log file list must not be null!")
  @NotEmpty(message = "Log file list must not be empty!")
  @Size(max = 30, message = "Log file list must not exceed 30 entries!")
  private List<String> logFiles;

  @Min(value = 1, message = "Parallel File Processing count must be greater than zero!")
  @Max(value = 15, message = "Parallel File Processing count must be at most 15!")
  private int parallelFileProcessingCount;

}
