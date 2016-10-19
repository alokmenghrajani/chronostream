package chronostream.common.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class AbstractJob<Config extends AbstractJobConfig, Result extends AbstractJobResult> implements Runnable {
  protected Config config;
  protected Result result;

  public AbstractJob(Config config) {
    this.config = config;
    this.result = initResult();
  }

  public void run() {
    try {
      // submit jobs
      final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(config.threads);
      ExecutorService executorService =
          new ThreadPoolExecutor(config.threads, config.threads, 0L, TimeUnit.MILLISECONDS, queue);

      for (int i=0; i<config.threads; i++) {
        executorService.submit(initTask());
      }
      // TODO: make the 5 minutes a config setting, handle timeouts in a better way?
      executorService.awaitTermination(5, TimeUnit.MINUTES);
    } catch (Exception e) {
      System.out.println(e);
      result.recordException(e);
    }
  }

  abstract protected Result initResult();

  abstract protected Runnable initTask();

  public Result getResult() {
    return result;
  }
}
