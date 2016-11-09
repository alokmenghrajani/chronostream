package chronostream.correctness;

import java.util.List;

/**
 * Runs all the tests sequentially and then wait for sleep ms.
 */
public class CorrectnessJobTask implements Runnable {
  private List<CorrectnessJobConfig> config;
  private int sleep;
  private CorrectnessJobResult result;

  CorrectnessJobTask(List<CorrectnessJobConfig> config, int sleep, CorrectnessJobResult result) {
    this.config = config;
    this.sleep = sleep;
    this.result = result;
  }

  public void run() {
    while (true) {
      for (CorrectnessJobConfig c : config) {
        try {
          c.doCrypto(result);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {
      }
      result.nextIteration();
    }
  }
}
