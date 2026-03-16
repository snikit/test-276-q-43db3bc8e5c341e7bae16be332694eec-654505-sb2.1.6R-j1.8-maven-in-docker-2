package org.codejudge.sb.models;


import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LogAnalyzeRequest {

  @NotEmpty
  @Size(max = 30)
  private List<String> logFiles;

  @Min(1)
  @Max(15)
  private int parallelFileProcessingCount;

}
