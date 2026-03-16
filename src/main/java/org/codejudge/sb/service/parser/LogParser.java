package org.codejudge.sb.service.parser;

import java.io.IOException;
import java.util.Map;
import org.codejudge.sb.models.ExceptionKey;
import org.codejudge.sb.models.TimeRange;

public interface LogParser {

  Map<TimeRange, Map<ExceptionKey, Long>> parse(String filePath) throws IOException;
}
