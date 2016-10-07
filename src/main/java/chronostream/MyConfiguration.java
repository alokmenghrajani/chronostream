package chronostream;

import chronostream.core.CryptoConfig;
import io.dropwizard.Configuration;
import java.util.List;

public class MyConfiguration extends Configuration {
  public List<CryptoConfig> crypto;
}
