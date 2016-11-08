package chronostream;

import chronostream.common.crypto.CryptoPrimitive;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import io.dropwizard.Configuration;
import java.util.List;

public class Config extends Configuration {
  // Info about providers
  public List<CryptoProvider> cryptoProviders;

  // Info about perf tests
  public PerfTest perfTest;

  // Info about correctness tests
  public CorrectnessTest correctnessTest;

  @AutoValue
  abstract static public class CryptoProvider {
    public abstract String name();
    public abstract String storeType();
    public abstract String provider();
    public abstract String password();
    public abstract String keyStore();
    public abstract boolean allowsExport();

    @JsonCreator @SuppressWarnings("unused")
    static CryptoProvider create(@JsonProperty("name") String name,
        @JsonProperty("storeType") String storeType,
        @JsonProperty("provider") String provider,
        @JsonProperty("password") String password,
        @JsonProperty("keyStore") String keyStore,
        @JsonProperty("allowsExport") boolean allowsExport) {
      return new AutoValue_Config_CryptoProvider(name, storeType, provider, password, keyStore, allowsExport);
    }

    public char[] pass() {
      return password().toCharArray();
    }
  }

  @AutoValue
  abstract static public class PerfTest {
    public abstract int defaultThreads();
    public abstract int defaultIterations();
    public abstract List<Test> tests();

    @JsonCreator @SuppressWarnings("unused")
    static PerfTest create(@JsonProperty("defaultThreads") int defaultThreads,
        @JsonProperty("defaultIterations") int defaultIterations,
        @JsonProperty("tests") List<Test> tests) {
      return new AutoValue_Config_PerfTest(defaultThreads, defaultIterations, tests);
    }
  }

  @AutoValue
  abstract static public class CorrectnessTest {
    abstract public int threads();
    abstract public int sleep(); // in ms
    public abstract List<Test> tests();

    @JsonCreator @SuppressWarnings("unused")
    static CorrectnessTest create(@JsonProperty("threads") int threads,
        @JsonProperty("sleep") int sleep,
        @JsonProperty("tests") List<Test> tests) {
      return new AutoValue_Config_CorrectnessTest(threads, sleep, tests);
    }
  }

  @AutoValue
  abstract static public class Test {
    public abstract String name();
    public abstract String primitive();
    public abstract int keySize();
    public abstract int minDataSize();
    public abstract int maxDataSize();

    @JsonCreator @SuppressWarnings("unused")
    static Test create(@JsonProperty("name") String name,
        @JsonProperty("primitive") String primitive,
        @JsonProperty("keySize") int keySize,
        @JsonProperty("minDataSize") int minDataSize,
        @JsonProperty("maxDataSize") int maxDataSize) {
      return new AutoValue_Config_Test(name, primitive, keySize, minDataSize, maxDataSize);
    }

    public CryptoPrimitive getPrimitive() {
      return CryptoPrimitive.valueOf(primitive());
    }
  }
}
