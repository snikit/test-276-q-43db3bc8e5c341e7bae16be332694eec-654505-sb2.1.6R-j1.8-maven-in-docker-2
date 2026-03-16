package org.codejudge.sb.service.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codejudge.sb.models.ExceptionKey;
import org.codejudge.sb.models.TimeRange;
import org.springframework.stereotype.Component;

@Component
public class BufferedReaderLogParser implements LogParser {

  private static final Pattern LOG_PATTERN = Pattern.compile(
      "^(\\d+)\\s+(\\d+)\\s+([\\w$.]+Exception[\\w$.]*)$");

  public Map<TimeRange, Map<ExceptionKey, Long>> parse(String filePath) throws IOException {
    final URLConnection connection;
    try {
      connection = URI.create(filePath).toURL().openConnection();
    } catch (IllegalArgumentException | java.net.MalformedURLException ex) {
      throw new IOException("Invalid or unsupported file URL [" + filePath + "]: " + ex.getMessage(), ex);
    }
    connection.setConnectTimeout(5_000); // 5s to establish connection
    connection.setReadTimeout(30_000); // 30s to read data

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

      return reader.lines()
          .map(String::trim)
          .map(LOG_PATTERN::matcher)
          .filter(Matcher::matches)
          .collect(Collectors.groupingBy(
              m -> bucketToQuarterHour(Long.parseLong(m.group(2))),
              Collectors.groupingBy(
                  m -> new ExceptionKey(m.group(3)),
                  Collectors.counting())));
    }
  }

  // 1523719007817 → "2018-04-14 15:15-15:30"
  private TimeRange bucketToQuarterHour(long epochMillis) {
    Instant instant = Instant.ofEpochMilli(epochMillis);
    LocalDateTime dt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

    int minute = dt.getMinute();
    int bucketStart = (minute / 15) * 15; // floor to 0, 15, 30, or 45
    int bucketEnd = bucketStart + 15;

    return new TimeRange(
        String.format("%02d:%02d-%02d:%02d", dt.getHour(), bucketStart,
            dt.getHour(), bucketEnd));
  }

}
