package org.codejudge.sb.api;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.codejudge.sb.models.ApiErrorResponse;
import org.codejudge.sb.models.LogAnalyzeRequest;
import org.codejudge.sb.models.LogAnalyzeResponse;
import org.codejudge.sb.service.LogAnalyzerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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

  // ── Validation failures (Bean Validation / @Valid) ──────────────────────────
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    // Pick the first constraint-violation message; order is deterministic via
    // bean-validation API.
    String reason = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Validation error")
        .orElse("Validation error");
    return ResponseEntity.badRequest().body(ApiErrorResponse.failure(reason));
  }

  // ── Explicit bad-argument errors thrown by service layer ────────────────────
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ApiErrorResponse.failure(ex.getMessage()));
  }

  // ── Catch-all — never leak a Spring whitepage on unexpected failures ─────────
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
    log.error("Unexpected error processing request", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse.failure("Internal server error. Please try again later."));
  }
}
