package chronostream.correctness;

import chronostream.common.core.AbstractJobConfig;
import chronostream.common.crypto.Crypto;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration options needed to start a correctness job.
 */
public class CorrectnessJobConfig extends AbstractJobConfig {
  protected List<Crypto> crypto;

  public CorrectnessJobConfig(List<Crypto> crypto, int threads) throws Exception {
    super(threads);

    // find a crypto which allows exporting keys
    Crypto exportFrom = null;
    for (Crypto c : crypto) {
      if (c.config.allowsExport()) {
        exportFrom = c;
        break;
      }
    }
    Objects.requireNonNull(exportFrom);

    this.crypto = new ArrayList<>();
    for (Crypto c : crypto) {
      this.crypto.add(new Crypto(c.config, exportFrom));
    }
  }
}
