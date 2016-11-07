package chronostream.correctness;

import chronostream.common.crypto.Crypto;
import java.util.Arrays;
import java.util.Random;

import static chronostream.common.crypto.CryptoPrimitive.AES256_CBC_ENC;
import static chronostream.common.crypto.CryptoPrimitive.HKDF;
import static chronostream.common.crypto.CryptoPrimitive.RSA_ENC;

/**
 * Runs a specific test for a specific number of iterations.
 */
public class CorrectnessJobTask implements Runnable {
  CorrectnessJobConfig config;
  CorrectnessJobResult result;

  CorrectnessJobTask(CorrectnessJobConfig config, CorrectnessJobResult result) {
    this.config = config;
    this.result = result;
  }

  public void run() {
    try {
      while (true) {
        int size = (new Random().nextInt() & 0x0fff) + 1;
        byte[] buffer = new byte[size];
        new Random().nextBytes(buffer);

        // Check that every Crypto gives the same output for HMAC.
        byte[] ref = this.config.crypto.get(0).doHKDF(buffer);
        for (Crypto c : this.config.crypto) {
          byte[] r = c.doHKDF(buffer);
          result.addResult(HKDF, c, c, Arrays.equals(ref, r));
        }

        // Check that AES encryption-decryption works with every pair of Crypto
        for (Crypto c1 : this.config.crypto) {
          byte[] iv = new byte[16];
          new Random().nextBytes(iv);
          byte[] t = c1.doAesCbcEncryption(buffer, iv);
          for (Crypto c2 : this.config.crypto) {
            byte[] r = c2.doAesCbcDecryption(t, iv);
            result.addResult(AES256_CBC_ENC, c1, c2, Arrays.equals(buffer, r));
          }
        }

        // Check RSA
        size = (new Random().nextInt() & 0x80) + 1;
        buffer = new byte[size];
        new Random().nextBytes(buffer);
        for (Crypto c1 : this.config.crypto) {
          byte[] t = c1.doRsaEncryption(buffer);
          for (Crypto c2 : this.config.crypto) {
            byte[] r = c2.doRsaDecryption(t);
            result.addResult(RSA_ENC, c1, c2, Arrays.equals(buffer, r));
          }
        }

        // record that we went through one iteration
        result.nextIteration();

        // Sleep for a bit
        Thread.sleep(100);
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.recordException(e);
    }
  }
}
