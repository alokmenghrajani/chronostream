package chronostream.perf;

import chronostream.common.core.AbstractJobConfig;
import chronostream.common.crypto.Crypto;
import chronostream.common.crypto.CryptoPrimitive;

/**
 * Configuration options needed to start a perf job.
 */
public class PerfJobConfig extends AbstractJobConfig {
  Crypto crypto;
  CryptoPrimitive cryptoPrimitive;
  int bytes;
  int iterations;

  public PerfJobConfig(Crypto crypto, CryptoPrimitive cryptoPrimitive, int bytes, int iterations, int threads) {
    super(threads);

    this.crypto = crypto;
    this.cryptoPrimitive = cryptoPrimitive;
    this.bytes = bytes;
    this.iterations = iterations;
  }
}
