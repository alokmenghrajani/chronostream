package chronostream.perf;

/**
 * Runs a specific test for a specific number of iterations.
 */
public class PerfJobTask implements Runnable {
  private PerfJobConfig test;
  private PerfJobResult result;
  private int iterations;

  PerfJobTask(PerfJobConfig test, int iterations, PerfJobResult result) {
    this.test = test;
    this.iterations = iterations;
    this.result = result;
  }

  public void run() {
    try {
      for (int i=0; i<iterations; i++) {
        test.doCrypto(result);
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.recordException(e);
    }
  }
}
