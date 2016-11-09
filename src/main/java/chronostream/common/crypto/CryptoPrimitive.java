package chronostream.common.crypto;

public enum CryptoPrimitive {
  AES_CBC_ENC("AES/CBC/PKCS5 encryption", 16),
  AES_CBC_DEC("AES/CBC/PKCS5 decryption", 16),
  HKDF("HKDF", 0),
  RSA_ENC("RSA encryption", 0),
  RSA_DEC("RSA decryption", 0);

  // todo: sign/verify?

  public String name;
  public int iv;

  CryptoPrimitive(String name, int iv) {
    this.name = name;
    this.iv = iv;
  }
}
