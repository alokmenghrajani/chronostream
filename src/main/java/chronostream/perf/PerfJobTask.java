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
      byte[] buffer1 = new byte[config.bytes];
      for (int i=0; i<config.iterations; i++) {
        // note: we don't count the time it takes to fill the random buffer
        new Random().nextBytes(buffer1);

        byte[] iv = new byte[16];
        new Random().nextBytes(iv);
        byte[] buffer2 = buffer1;
        if (config.cryptoPrimitive == CryptoPrimitive.AES128_GCM_DEC) {
          buffer2 = config.crypto.doAesGcmEncryption(buffer1, iv);
        }
        if (config.cryptoPrimitive == CryptoPrimitive.AES256_CBC_DEC) {
          buffer2 = config.crypto.doAesCbcEncryption(buffer1, iv);
        }
        if (config.cryptoPrimitive == CryptoPrimitive.RSA_DEC) {
          buffer2 = config.crypto.doRsaEncryption(buffer1);
        }

        long start = System.nanoTime();
        config.crypto.doCrypto(config.cryptoPrimitive, buffer2, iv);
        long end = System.nanoTime();

        result.addResult(start/1000000, end/1000000);
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.recordException(e);
    }
  }
}
