package org.codejudge.sb.service;

import org.codejudge.sb.models.LogAnalyzeRequest;
import org.codejudge.sb.models.LogAnalyzeResponse;

public interface AnalyzerService {

  LogAnalyzeResponse analyze(LogAnalyzeRequest request);

}
