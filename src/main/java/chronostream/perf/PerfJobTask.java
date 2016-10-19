package chronostream.perf;

import chronostream.common.crypto.CryptoPrimitive;
import java.util.Random;

/**
 * Runs a specific test for a specific number of iterations.
 */
public class PerfJobTask implements Runnable {
  PerfJobConfig config;
  PerfJobResult result;

  PerfJobTask(PerfJobConfig config, PerfJobResult result) {
    this.config = config;
    this.result = result;
  }

  public void run() {
    try {
      byte[] buffer = new byte[config.bytes];
      for (int i=0; i<config.iterations; i++) {
        // note: we don't count the time it takes to fill the random buffer
        new Random().nextBytes(buffer);

        byte[] iv = new byte[16];
        new Random().nextBytes(iv);
        if (config.cryptoPrimitive == CryptoPrimitive.AES128GCM_DEC) {
          buffer = config.crypto.doCrypto(CryptoPrimitive.AES128GCM_ENC, buffer, iv);
        }

        long start = System.nanoTime();
        config.crypto.doCrypto(config.cryptoPrimitive, buffer, iv);
        long end = System.nanoTime();

        result.addResult(start/1000000, end/1000000);
      }
    } catch (Exception e) {
      System.out.println(e);
      result.recordException(e);
    }
  }
}
