package chronostream.common.crypto;

import com.google.common.base.MoreObjects;

public class CryptoConfig {
  public String name;
  public String storeType;
  public String provider;
  public String password;
  public String keyStore;

  public char[] pass() {
    return password.toCharArray();
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("storeType", storeType)
        .add("provider", provider)
        .add("password", password)
        .add("keyStore", keyStore)
        .toString();
  }
}