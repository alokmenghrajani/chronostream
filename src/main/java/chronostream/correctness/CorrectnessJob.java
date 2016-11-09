package chronostream.correctness;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Schedules a correctness job and gathers results.
 */
public class CorrectnessJob implements Runnable {
  private List<CorrectnessJobConfig> config;
  private CorrectnessJobResult result;
  private int threads;
  private int sleep;

  public CorrectnessJob(Config.CorrectnessTest config, List<CryptoProvider> providers) throws Exception {
    this.config = Lists.newArrayList();
    for (Config.Test test : config.tests()) {
      this.config.add(new CorrectnessJobConfig(test, providers));
    }
    result = new CorrectnessJobResult(config, providers);
    this.threads = config.threads();
    this.sleep = config.sleep();
  }

  public CorrectnessJobResult getResult() {
    return result;
  }

  public void run() {
    try {
      // create a fixed set of threads, which run the correctness jobs forever
      for (int i=0; i<threads; i++) {
        Thread t = new Thread(new CorrectnessJobTask(config, sleep, result));
        t.setName(String.format("correctnessJob-%d", i));
        t.start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
