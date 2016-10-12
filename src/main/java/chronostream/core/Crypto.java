package chronostream.core;

import chronostream.resources.Tests;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static java.lang.String.format;

public class Crypto {
  private CryptoConfig config;
  private Provider provider;
  private Key key;
  private Hkdf hkdf;

  public Crypto(CryptoConfig config) throws Exception {
    this.config = config;
    System.out.println(format("Initializing: %s", config));

    // create Provider
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
    KeyGenerator keyGen = KeyGenerator.getInstance(hmacAlg, provider);
    keyGen.init(256);
    SecretKey secretKey = keyGen.generateKey();

    // save the key
    KeyStore.PasswordProtection protectionParameter = new KeyStore.PasswordProtection(config.pass());
    keyStore.setEntry("hmacKey", new KeyStore.SecretKeyEntry(secretKey), protectionParameter);

    // create an AES key
    Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    int maxAllowedKeyLength = aesCipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding");
    System.out.println(format("Provider: %s, maxAllowedKeyLength: %d", config.name, maxAllowedKeyLength));

    keyGen = KeyGenerator.getInstance("AES", provider);
    keyGen.init(Math.min(128, maxAllowedKeyLength));
    secretKey = keyGen.generateKey();

    // save the key
    protectionParameter = new KeyStore.PasswordProtection(config.pass());
    keyStore.setEntry("aesKey", new KeyStore.SecretKeyEntry(secretKey), protectionParameter);

    // save the keystore
    keyStore.store(new FileOutputStream(config.keyStore), config.pass());
  }

  // HKDF

  public static void prepareHKDF(Crypto instance, Tests.TestResult result) {
    instance._prepareHKDF(result);
  }

  private void _prepareHKDF(Tests.TestResult result) {
    try {
      KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
      InputStream readStream = new FileInputStream(config.keyStore);
      keyStore.load(readStream, config.pass());
      readStream.close();
      key = keyStore.getKey("hmacKey", config.pass());
      hkdf = new Hkdf(provider);
    } catch (Exception e) {
      result.exception = e;
    }
  }

  public static void doHKDF(Crypto instance, Tests.TestResult result) {
    // preparation
    instance._doHKDF(result);
  }

  private void _doHKDF(Tests.TestResult result) {
    try {
      hkdf.expand((SecretKey) key, "hello world".getBytes(), 16);
    } catch (Exception e) {
      result.exception = e;
    }
  }

  // AES encryption

  public static void prepareAesEncryption(Crypto instance, Tests.TestResult result) {
    instance._prepareAesEncryption(result);
  }

  private void _prepareAesEncryption(Tests.TestResult result) {
    try {
      KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
      InputStream readStream = new FileInputStream(config.keyStore);
      keyStore.load(readStream, config.pass());
      readStream.close();
      key = keyStore.getKey("aesKey", config.pass());
    } catch (Exception e) {
      result.exception = e;
    }
  }

  public static void doAesEncryption(Crypto instance, Tests.TestResult result) {
    instance._doAesEncryption(result);
  }

  private void _doAesEncryption(Tests.TestResult result) {
    try {
      Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
      aesCipher.init(Cipher.ENCRYPT_MODE, key);
      aesCipher.update("4444444444444444".getBytes());
      aesCipher.doFinal();
    } catch (Exception e) {
      result.exception = e;
    }
  }
}
