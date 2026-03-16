package org.codejudge.sb.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codejudge.sb.models.FileResult;
import org.codejudge.sb.models.LogAnalyzeRequest;
import org.codejudge.sb.models.LogAnalyzeResponse;
import org.codejudge.sb.service.aggregator.ResultAggregator;
import org.codejudge.sb.service.parser.LogParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalyzerService implements AnalyzerService {

  private final LogParser logParser;

  @Qualifier("logProcessorExecutor")
  private final Executor executor;

  private final ResultAggregator resultAggregator;

  @Override
  public LogAnalyzeResponse analyze(LogAnalyzeRequest request) {

    List<String> files = request.getLogFiles();

    // Defensive guard — Bean Validation should catch this first, but protect at
    // service level too.
    if (request.getParallelFileProcessingCount() <= 0) {
      throw new IllegalArgumentException(
          "Parallel File Processing count must be greater than zero!");
    }

    int parallelCount = Math.min(request.getParallelFileProcessingCount(), files.size());

    // Shared atomic index — each worker claims next file by incrementing this
    AtomicInteger nextIndex = new AtomicInteger(0);

    // Collect per-file results as they complete
    List<CompletableFuture<List<FileResult>>> futures = new ArrayList<>();

    // Spawn exactly parallelCount workers; each loops until the queue is empty
    for (int i = 0; i < parallelCount; i++) {
      CompletableFuture<List<FileResult>> workerFuture = CompletableFuture
          .supplyAsync(() -> processUntilEmpty(files, nextIndex), executor);
      futures.add(workerFuture);
    }

    try {
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    } catch (CompletionException ex) {
      // Unwrap the root cause so the global handler can log & surface it clearly.
      Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
      log.error("One or more worker threads failed during log processing", cause);
      throw new RuntimeException("Log processing failed: " + cause.getMessage(), cause);
    }

    // Flatten results from all workers
    List<FileResult> allResults = futures.stream()
        .map(CompletableFuture::join)
        .flatMap(List::stream)
        .toList();

    return resultAggregator.aggregate(allResults);
  }

  /**
   * A single worker: keeps claiming files until none remain.
   * File-level errors are captured as {@link FileResult#error} rather than
   * thrown,
   * so one bad file never aborts the entire batch.
   */
  private List<FileResult> processUntilEmpty(List<String> files, AtomicInteger nextIndex) {
    List<FileResult> results = new ArrayList<>();

    while (true) {
      int idx = nextIndex.getAndIncrement(); // atomically claim next slot
      if (idx >= files.size()) {
        break; // no more files
      }

      String file = files.get(idx);
      try {
        var entries = logParser.parse(file);
        results.add(FileResult.success(file, entries));
      } catch (Exception e) {
        log.warn("Failed to parse file [{}]: {}", file, e.getMessage());
        results.add(FileResult.error(file, e.getMessage() != null ? e.getMessage()
            : e.getClass().getSimpleName()));
      }
    }
    return results;
  }
}
