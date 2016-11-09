package chronostream.perf;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/**
 * Schedules a perf job and gathers results.
 */
public class PerfJob implements Runnable {
  private AtomicInteger threads;
  private AtomicInteger iterations;
  private List<PerfJobConfig> perfJobConfigs;
  private Map<Integer, PerfJobResult> results = Maps.newHashMap();
  private PrintStream ps;

  public PerfJob(Config.PerfTest config, List<CryptoProvider> providers) throws Exception {
    this.threads = new AtomicInteger(config.defaultThreads());
    this.iterations = new AtomicInteger(config.defaultIterations());

    perfJobConfigs = Lists.newArrayList();
    for (CryptoProvider provider : providers) {
      for (Config.Test test : config.tests()) {
        perfJobConfigs.add(new PerfJobConfig(test, provider));
      }
    }

    ZonedDateTime now = ZonedDateTime.now();
    ps = new PrintStream(new FileOutputStream(String.format("%s.log", now.format(ISO_LOCAL_DATE_TIME))));
  }

  public PerfJobResult getResult(int id) {
    return results.get(id);
  }

  public void run() {
    int id = 0;
    while (true) {
      id++;
      for (PerfJobConfig test : perfJobConfigs) {
        int n_threads = this.threads.get();
        int iterations = this.iterations.get();
        PerfJobResult result = new PerfJobResult(id,
            String.format("%s-%s", test.config.name(), test.provider.getName()),
            n_threads,
            iterations,
            n_threads * iterations,
            ps);
        if (id < 5) {
          results.put(id, result);
        }
        //todo: results.remove(id-5) if id>5;

        // Spin up the dynamically configured number of threads
        Thread[] threads = new Thread[n_threads];
        for (int i = 0; i < n_threads; i++) {
          threads[i] = new Thread(new PerfJobTask(test, iterations, result));
          threads[i].setName(String.format("perfJob-%d", i));
          threads[i].start();
        }

        // Wait for all threads to finish
        for (int i = 0; i < n_threads; i++) {
          try {
            threads[i].join();
            // for some reason, a few threads are enough to make my laptop fan spin, so sleep some.
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
            result.recordException(e);
          }
        }

        try {
          result.write();
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    }
  }
}
