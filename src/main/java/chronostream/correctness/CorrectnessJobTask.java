package chronostream.correctness;

import chronostream.common.crypto.Crypto;
import chronostream.common.crypto.CryptoPrimitive;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.tuple.Pair;

import static chronostream.common.crypto.CryptoPrimitive.AES128GCM_DEC;
import static chronostream.common.crypto.CryptoPrimitive.AES128GCM_ENC;
import static chronostream.common.crypto.CryptoPrimitive.HKDF;
import static chronostream.common.crypto.CryptoPrimitive.RSA_DEC;
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
      for (int i=0; i<config.iterations; i++) {
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
          byte[] t = c1.doAesEncryption(buffer, iv);
          for (Crypto c2 : this.config.crypto) {
            byte[] r = c2.doAesDecryption(t, iv);
            result.addResult(AES128GCM_ENC, c1, c2, Arrays.equals(buffer, r));
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
      }
    } catch (Exception e) {
      e.printStackTrace();
      result.recordException(e);
    }
  }
}
