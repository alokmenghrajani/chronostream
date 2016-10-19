package chronostream.common.crypto;

public enum CryptoPrimitive {
  AES128GCM_ENC("AES-128/GCM/NoPadding encryption"),
  AES128GCM_DEC("AES-128/GCM/NoPadding decryption"),
  HKDF("HKDF"),
  RSA_ENC("RSA encryption"),
  RSA_DEC("RSA decryption"),
  RSA_SIGN("RSA signing"),
  RSA_VERIFY("RSA verify");

  public String name;

  CryptoPrimitive(String name) {
    this.name = name;
  }
}
