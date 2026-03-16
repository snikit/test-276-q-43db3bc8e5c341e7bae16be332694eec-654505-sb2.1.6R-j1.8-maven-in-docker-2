package org.codejudge.sb.api;


import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.codejudge.sb.models.LogAnalyzeRequest;
import org.codejudge.sb.models.LogAnalyzeResponse;
import org.codejudge.sb.service.LogAnalyzerService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogAnalyzerAPI {


  private final LogAnalyzerService analyzerService;

  public LogAnalyzerAPI(LogAnalyzerService analyzerService) {
    this.analyzerService = analyzerService;
  }

  @PostMapping("/api/process-logs/")
  public ResponseEntity<LogAnalyzeResponse> analyze(@Valid @RequestBody LogAnalyzeRequest request) {
    return ResponseEntity.ok(analyzerService.analyze(request));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(FieldError::getField,
            error -> Objects.isNull(error.getDefaultMessage()) ? "ERROR"
                : error.getDefaultMessage()));
    return ResponseEntity.badRequest().body(errors);
  }
}
