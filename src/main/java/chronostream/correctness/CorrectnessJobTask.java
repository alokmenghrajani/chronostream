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
import static chronostream.common.crypto.CryptoPrimitive.RSA_SIGN;
import static chronostream.common.crypto.CryptoPrimitive.RSA_VERIFY;

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
        int size = new Random().nextInt() % 1024;
        byte[] buffer = new byte[size];
        new Random().nextBytes(buffer);

        // Check that every Crypto gives the same output for HMAC.
        byte[] ref = this.config.cryptos.get(0).doHKDF(buffer);
        for (Crypto c : this.config.cryptos) {
          byte[] r = c.doHKDF(buffer);
          result.addResult(HKDF, c, c, Arrays.equals(ref, r));
        }

        // Check that encryption-decryption works with every pair of Crypto
        List<Pair<CryptoPrimitive, CryptoPrimitive>> operations = ImmutableList.of(
            Pair.of(AES128GCM_ENC, AES128GCM_DEC),
            Pair.of(RSA_ENC, RSA_DEC),
            Pair.of(RSA_SIGN, RSA_VERIFY));
        for (Pair<CryptoPrimitive, CryptoPrimitive> op : operations) {
          for (Crypto c1 : this.config.cryptos) {
            byte[] iv = new byte[16];
            new Random().nextBytes(iv);
            byte[] t = c1.doCrypto(op.getLeft(), buffer, iv);
            for (Crypto c2 : this.config.cryptos) {
              byte[] r = c2.doCrypto(op.getRight(), t, iv);
              result.addResult(op.getLeft(), c1, c2, Arrays.equals(buffer, r));
            }
          }
        }

        // record that we went through one iteration
        result.nextIteration();
      }
    } catch (Exception e) {
      System.out.println(e);
      result.recordException(e);
    }
  }
}
