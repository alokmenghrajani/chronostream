package chronostream;

import chronostream.common.crypto.CryptoConfig;
import io.dropwizard.Configuration;
import java.util.List;

public class Config extends Configuration {
  public List<CryptoConfig> crypto;

  // Number of threads to use for correctness tests
  public int correctnessThreads;
}
