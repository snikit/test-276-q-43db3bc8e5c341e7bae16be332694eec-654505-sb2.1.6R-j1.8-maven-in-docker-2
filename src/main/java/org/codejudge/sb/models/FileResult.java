package org.codejudge.sb.models;

import java.util.Map;

public record FileResult(String file, Map<TimeRange, Map<ExceptionKey, Long>> entries,
                         String error) {

  public static FileResult success(String f, Map<TimeRange, Map<ExceptionKey, Long>> e) {
    return new FileResult(f, e, null);
  }

  public static FileResult error(String f, String err) {
    return new FileResult(f, Map.of(), err);
  }

  public boolean hasError() {
    return error != null;
  }
}
