package chronostream.perf;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/**
 * Schedules a perf job and gathers results.
 */
public class PerfJob implements Runnable {
  private int threads;
  private int total;
  private List<PerfJobConfig> perfJobConfigs;
  private Map<Integer, PerfJobResult> results = Maps.newHashMap();
  private String filename;

  public PerfJob(Config.PerfTest config, List<CryptoProvider> providers) throws Exception {
    this.threads = config.defaultThreads();
    this.total = config.defaultTotal();

    perfJobConfigs = Lists.newArrayList();
    for (CryptoProvider provider : providers) {
      for (Config.Test test : config.tests()) {
        perfJobConfigs.add(new PerfJobConfig(test, provider));
      }
    }

    ZonedDateTime now = ZonedDateTime.now();
    filename = String.format("%s", now.format(ISO_LOCAL_DATE_TIME));
  }

  public PerfJobResult getResult(int id) {
    return results.get(id);
  }

  public void run() {
    int id = 0;
    threads = 1;
    while (threads < 50) {
      id++;
      for (PerfJobConfig test : perfJobConfigs) {
        int iterations = total / threads;
        PerfJobResult result = new PerfJobResult(id,
            String.format("%s-%s", test.config.name(), test.provider.getName()),
            threads,
            iterations,
            threads * iterations,
            filename);
//        if (id < 5) {
//          results.put(id, result);
//        }
//        //todo: results.remove(id-5) if id>5;

        // Spin up the dynamically configured number of threads
        Thread[] threads_arr = new Thread[threads];
        for (int i = 0; i < threads; i++) {
          threads_arr[i] = new Thread(new PerfJobTask(test, iterations, result));
          threads_arr[i].setName(String.format("perfJob-%d", i));
          threads_arr[i].start();
        }

        // Wait for all threads to finish
        for (int i = 0; i < threads; i++) {
          try {
            threads_arr[i].join();
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
      threads++;
    }
  }
}
