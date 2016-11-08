package chronostream.perf;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schedules a perf job and gathers results.
 */
public class PerfJob implements Runnable {
  private AtomicInteger threads;
  private AtomicInteger iterations;
  private List<PerfJobConfig> perfJobConfigs;
  private PerfJobResult result;

  public PerfJob(Config.PerfTest config, List<CryptoProvider> providers) throws Exception {
    this.threads = new AtomicInteger(config.defaultThreads());
    this.iterations = new AtomicInteger(config.defaultIterations());

    perfJobConfigs = Lists.newArrayList();
    for (CryptoProvider provider : providers) {
      for (Config.Test test : config.tests()) {
        perfJobConfigs.add(new PerfJobConfig(test, provider));
      }
    }

    result = new PerfJobResult();
  }

  public void run() {
    while (true) {
      for (PerfJobConfig test : perfJobConfigs) {
        int n_threads = this.threads.get();
        int iterations = this.iterations.get();
        result.prepare(n_threads * iterations);

        // Spin up the dynamically configured number of threads
        Thread[] threads = new Thread[n_threads];
        for (int i = 0; i < n_threads; i++) {
          threads[i] = new Thread(new PerfJobTask(test, iterations, result));
          threads[i].start();
        }

        // Wait for all threads to finish
        for (int i = 0; i < n_threads; i++) {
          try {
            threads[i].join();
          } catch (InterruptedException e) {
            e.printStackTrace();
            result.recordException(e);
          }
        }

        result.flush();
      }
    }
  }
}
