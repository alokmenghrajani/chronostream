package chronostream.common.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
abstract public class CryptoConfig {
  abstract public String name();
  abstract public String storeType();
  abstract public String provider();
  abstract public String password();
  abstract public String keyStore();
  abstract public boolean allowsExport();

  @JsonCreator @SuppressWarnings("unused")
  static CryptoConfig create(@JsonProperty("name") String name,
      @JsonProperty("storeType") String storeType,
      @JsonProperty("provider") String provider,
      @JsonProperty("password") String password,
      @JsonProperty("keyStore") String keyStore,
      @JsonProperty("allowsExport") boolean allowsExport) {
    return new AutoValue_CryptoConfig(name, storeType, provider, password, keyStore, allowsExport);
  }

  public char[] pass() {
    return password().toCharArray();
  }
}