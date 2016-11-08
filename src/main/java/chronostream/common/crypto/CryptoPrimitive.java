package chronostream.common.crypto;

public enum CryptoPrimitive {
  AES_CBC_ENC("AES/CBC/PKCS5 encryption"),
  AES_CBC_DEC("AES/CBC/PKCS5 decryption"),
  HKDF("HKDF"),
  RSA_ENC("RSA encryption"),
  RSA_DEC("RSA decryption");

  // todo: sign/verify?

  public String name;

  CryptoPrimitive(String name) {
    this.name = name;
  }
}
