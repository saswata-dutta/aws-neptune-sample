package org.saswata;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BatchExecutor implements AutoCloseable {
  private final PrintWriter out;
  private final int BATCH_SIZE;

  private final ExecutorService executorService;
  private final CompletionService<String> completionService;
  private final ArrayList<Callable<String>> pendingTasks;

  public BatchExecutor(PrintWriter out, int batch_size) {
    this.out = out;
    BATCH_SIZE = batch_size;

    executorService = Executors.newFixedThreadPool(BATCH_SIZE);
    completionService = new ExecutorCompletionService<>(executorService);

    pendingTasks = new ArrayList<>();
  }

  public void submit(Callable<String> task) {
    pendingTasks.add(task);
    if (pendingTasks.size() >= BATCH_SIZE) execute();
  }

  private void execute() {
    pendingTasks.forEach(completionService::submit);

    for (int i = 0; i < pendingTasks.size(); ++i) {
      try {
        Future<String> result = completionService.take();
        out.println(result.get());
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      }
    }

    pendingTasks.clear();
  }

  @Override
  public void close() throws Exception {
    execute();
    executorService.shutdown();
    if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
      System.err.println("Waited for 10 seconds, forced exit");
      System.exit(0);
    }
  }
}
