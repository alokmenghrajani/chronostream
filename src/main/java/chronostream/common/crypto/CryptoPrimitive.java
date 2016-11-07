package chronostream.common.crypto;

public enum CryptoPrimitive {
  AES256_CBC_ENC("AES-256/CBC/PKCS5 encryption"),
  AES256_CBC_DEC("AES-256/CBC/PKCS5 decryption"),
  HKDF("HKDF"),
  RSA_ENC("RSA encryption"),
  RSA_DEC("RSA decryption");

  //todo: sign/verify?

  public String name;

  CryptoPrimitive(String name) {
    this.name = name;
  }
}
