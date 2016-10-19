package chronostream.correctness;

import chronostream.common.core.AbstractJobConfig;
import chronostream.common.crypto.Crypto;
import java.util.List;

/**
 * Configuration options needed to start a correctness job.
 */
public class CorrectnessJobConfig extends AbstractJobConfig {
  List<Crypto> cryptos;
  int iterations;

  public CorrectnessJobConfig(List<Crypto> cryptos, int iterations, int threads) {
    super(threads);

    this.cryptos = cryptos;
    this.iterations = iterations;
  }
}
