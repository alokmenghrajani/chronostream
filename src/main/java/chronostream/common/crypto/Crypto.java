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
  private SecretKey aesKey;
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
    keyGen.init(Math.min(128, maxAllowedKeyLength));
    aesKey = keyGen.generateKey();

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
    this.aesKey = new SecretKeySpec(importFrom.aesKey.getEncoded(), "AES");
    this.rsaKey = importFrom.rsaKey;
    // todo: rsa key
  }

  public byte[] doCrypto(CryptoPrimitive primitive, byte[] buffer, byte[] iv) throws Exception {
    switch (primitive) {
      case AES128GCM_ENC:
        return doAesEncryption(buffer, iv);
      case AES128GCM_DEC:
        return doAesDecryption(buffer, iv);
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

  public byte[] doAesEncryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  public byte[] doAesDecryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

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
