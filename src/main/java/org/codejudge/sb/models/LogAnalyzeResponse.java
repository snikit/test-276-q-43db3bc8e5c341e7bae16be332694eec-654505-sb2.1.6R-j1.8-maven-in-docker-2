package org.codejudge.sb.models;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogAnalyzeResponse {

  private List<FileParseResult> response;
  private List<FileError> errors;

  public static LogAnalyzeResponse toResponse(
      Map<TimeRange, Map<ExceptionKey, Long>> parsed,
      List<FileError> errors) {
    List<LogAnalyzeResponse.FileParseResult> results = parsed.entrySet().stream()
        .map(entry -> {
          String timestamp = entry.getKey().range(); // e.g. "2018-04-14 15:15-15:30"
          List<ExceptionLog> logs = entry.getValue().entrySet().stream()
              .map(e -> new ExceptionLog(e.getKey().key(), e.getValue()))
              .collect(Collectors.toList());
          return new LogAnalyzeResponse.FileParseResult(timestamp, logs);
        })
        .collect(Collectors.toList());

    return new LogAnalyzeResponse(results, errors);
  }

  public record FileParseResult(String timestamp, List<ExceptionLog> logs) {

  }

  public record ExceptionLog(String exception, long count) {

  }

  public record FileError(String file, String error) {

  }
}
