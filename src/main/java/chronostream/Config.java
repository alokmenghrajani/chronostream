package chronostream;

import chronostream.core.CryptoConfig;
import io.dropwizard.Configuration;
import java.util.List;

public class Config extends Configuration {
  public List<CryptoConfig> crypto;
}
