package org.codejudge.sb.service;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.codejudge.sb.models.FileResult;
import org.codejudge.sb.models.LogAnalyzeRequest;
import org.codejudge.sb.models.LogAnalyzeResponse;
import org.codejudge.sb.service.aggregator.ResultAggregator;
import org.codejudge.sb.service.parser.LogParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogAnalyzerService implements AnalyzerService {


  private final LogParser logParser;

  @Qualifier("logProcessorExecutor")
  private final Executor executor;

  @Autowired
  private final ResultAggregator resultAggregator;

  @Override
  public LogAnalyzeResponse analyze(LogAnalyzeRequest request) {

    List<String> files = request.getLogFiles();
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

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

    // Wait for all workers, then flatten results
    List<FileResult> allResults = futures.stream()
        .map(CompletableFuture::join)          // blocks until each worker finishes
        .flatMap(List::stream)
        .toList();

    return resultAggregator.aggregate(allResults);
  }

  /**
   * A single worker: keeps claiming files until none remain. This is what gives us the
   * self-replenishing pool — a fast worker doesn't wait for slow siblings.
   */
  private List<FileResult> processUntilEmpty(List<String> files, AtomicInteger nextIndex) {
    List<FileResult> results = new ArrayList<>();

    while (true) {
      int idx = nextIndex.getAndIncrement();  // atomically claim next slot
      if (idx >= files.size()) {
        break;         // no more files
      }

      String file = files.get(idx);
      try {
        var entries = logParser.parse(file);
        results.add(FileResult.success(file, entries));
      } catch (Exception e) {
        results.add(FileResult.error(file, e.getMessage()));
      }
    }
    return results;
  }
}
