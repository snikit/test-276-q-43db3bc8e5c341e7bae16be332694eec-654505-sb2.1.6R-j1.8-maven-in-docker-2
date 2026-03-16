package org.codejudge.sb.service.aggregator;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.codejudge.sb.models.ExceptionKey;
import org.codejudge.sb.models.FileResult;
import org.codejudge.sb.models.LogAnalyzeResponse;
import org.codejudge.sb.models.TimeRange;
import org.springframework.stereotype.Component;

@Component
public class ResultAggregator {

  public LogAnalyzeResponse aggregate(List<FileResult> fileResults) {
    List<LogAnalyzeResponse.FileError> errors = fileResults.stream().filter(FileResult::hasError)
        .map(
            result -> new LogAnalyzeResponse.FileError(result.file(), result.error())

        ).toList();

    Map<TimeRange, Map<ExceptionKey, Long>> aggregator = fileResults.stream()
        .filter(r -> !r.hasError()) // skip error results — entries map is empty but guard explicitly
        .map(FileResult::entries)
        .filter(map -> map != null && !map.isEmpty()) // null-safe guard
        .flatMap(map -> map.entrySet().stream()).collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (innerMap1, innerMap2) -> {
              // Merge inner maps on key collision — sum the counts
              Map<ExceptionKey, Long> merged = new HashMap<>(innerMap1);
              innerMap2.forEach((ex, count) -> merged.merge(ex, count, Long::sum));
              return merged;
            },
                () -> new TreeMap<>(
                    Comparator.comparing(t -> LocalTime.parse(t.range().split("-")[0])))));

    return LogAnalyzeResponse.toResponse(aggregator, errors);
  }
}