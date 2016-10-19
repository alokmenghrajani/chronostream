package chronostream.common.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static java.lang.String.format;

public class Crypto {
  public CryptoConfig config;
  private Provider provider;
  private Hkdf hkdf;
  private SecretKey hmacKey;
  private SecretKey aesKey128, aesKey256;
  private KeyPair rsaKey;

  public Crypto(CryptoConfig config) throws Exception {
    this.config = config;
    System.out.println(format("Initializing: %s", config));

    provider = (Provider)(Class.forName(config.provider).newInstance());
    Security.addProvider(provider);

    KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);

    String hmacAlg = "HmacSHA256";
    if (provider.getClass().getName().equals("com.safenetinc.luna.provider.LunaProvider")) {
      // Luna's keystore is slightly different from other providers. The keystore file proxies the login
      // state in some cases, but not always. It's better to use the login() method on LunaSlotManager.
      // We don't want to depend on Luna's provider jar, so we use reflection.
      Class slotManagerClass = Class.forName("com.safenetinc.luna.LunaSlotManager");
      Object slotManager = slotManagerClass.getMethod("getInstance", null).invoke(null, null);
      slotManagerClass.getMethod("login", String.class).invoke(slotManager, config.password);

      // Luna has a minor quirk, we have to create the hmac key as HmacSHA1.
      hmacAlg = "HmacSHA1";
    }

    // create an empty keystore
    keyStore.load(null, config.pass());

    // In some cases, the keystore isn't really empty (if the Provider decides to load keys from the HSM).
    if (keyStore.containsAlias("hmacKey")) {
      keyStore.deleteEntry("hmacKey");
    }
    if (keyStore.containsAlias("aesKey")) {
      keyStore.deleteEntry("aesKey");
    }

    // create a HmacSHA256 key, 32 bytes
    hkdf = new Hkdf(provider);
    KeyGenerator keyGen = KeyGenerator.getInstance(hmacAlg, provider);
    keyGen.init(256);
    hmacKey = keyGen.generateKey();

    // create an AES key
    Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    int maxAllowedKeyLength = aesCipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding");
    System.out.println(format("Provider: %s, maxAllowedKeyLength: %d", config.name, maxAllowedKeyLength));

    keyGen = KeyGenerator.getInstance("AES", provider);
    keyGen.init(128);
    aesKey128 = keyGen.generateKey();

    keyGen = KeyGenerator.getInstance("AES", provider);
    keyGen.init(256);
    aesKey256 = keyGen.generateKey();

    // create RSA key
    // note: JCE doesn't support rsa out of the box?
    if (!provider.getClass().getName().equals("com.sun.crypto.provider.SunJCE")) {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
      RSAKeyGenParameterSpec rsaKeyGenParameterSpec =
          new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
      generator.initialize(rsaKeyGenParameterSpec);
      rsaKey = generator.generateKeyPair();
    }
  }

  /**
   * Alternate constructor used for key-import.
   */
  public Crypto(CryptoConfig config, Crypto importFrom) throws Exception {
    this.config = config;
    System.out.println(format("re-initializing: %s", config));

    // create Provider
    provider = (Provider)(Class.forName(config.provider).newInstance());
    hkdf = new Hkdf(provider);

    String hmacAlg = "HmacSHA256";
    if (provider.getClass().getName().equals("com.safenetinc.luna.provider.LunaProvider")) {
      // Luna's keystore is slightly different from other providers. The keystore file proxies the login
      // state in some cases, but not always. It's better to use the login() method on LunaSlotManager.
      // We don't want to depend on Luna's provider jar, so we use reflection.
      Class slotManagerClass = Class.forName("com.safenetinc.luna.LunaSlotManager");
      Object slotManager = slotManagerClass.getMethod("getInstance", null).invoke(null, null);
      slotManagerClass.getMethod("login", String.class).invoke(slotManager, config.password);

      // Luna has a minor quirk, we have to create the hmac key as HmacSHA1.
      hmacAlg = "HmacSHA1";
    }

    this.hmacKey = new SecretKeySpec(importFrom.hmacKey.getEncoded(), hmacAlg);
    this.aesKey128 = new SecretKeySpec(importFrom.aesKey128.getEncoded(), "AES");
    this.aesKey256 = new SecretKeySpec(importFrom.aesKey256.getEncoded(), "AES");
    this.rsaKey = importFrom.rsaKey;
  }

  public byte[] doCrypto(CryptoPrimitive primitive, byte[] buffer, byte[] iv) throws Exception {
    switch (primitive) {
      case AES128_GCM_ENC:
        return doAesGcmEncryption(buffer, iv);
      case AES128_GCM_DEC:
        return doAesGcmDecryption(buffer, iv);
      case AES256_CBC_ENC:
        return doAesCbcEncryption(buffer, iv);
      case AES256_CBC_DEC:
        return doAesCbcDecryption(buffer, iv);
      case HKDF:
        return doHKDF(buffer);
      case RSA_ENC:
        return doRsaEncryption(buffer);
      case RSA_DEC:
        return doRsaDecryption(buffer);
    }
    throw new Exception("unreachable");
  }

  public byte[] doHKDF(byte[] bytes) throws Exception {
    return hkdf.expand(hmacKey, bytes, 16);
  }

  // AES GCM

  public byte[] doAesGcmEncryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/Nopadding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, aesKey128, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  public byte[] doAesGcmDecryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/Nopadding", provider);
    cipher.init(Cipher.DECRYPT_MODE, aesKey128, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  // AES CBC

  public byte[] doAesCbcEncryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, aesKey256, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  public byte[] doAesCbcDecryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, aesKey256, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  // RSA

  public byte[] doRsaEncryption(byte[] bytes) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, rsaKey.getPublic());
    return cipher.doFinal(bytes);
  }

  public byte[] doRsaDecryption(byte[] bytes) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, rsaKey.getPrivate());
    return cipher.doFinal(bytes);
  }
}
