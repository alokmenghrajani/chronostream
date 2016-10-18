package chronostream.core;

import chronostream.resources.Tests;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static java.lang.String.format;

public class Crypto {
  private CryptoConfig config;
  private Provider provider;
  private Key hmacKey;
  private Hkdf hkdf;
  private Key aesKey;
  private KeyPair rsaKey;

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

    this.prepareHKDF();
    this.prepareAesEncryption();

    // create RSA key
    // JCE doesn't support rsa out of the box?
    if (!provider.getClass().getName().equals("com.sun.crypto.provider.SunJCE")) {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
      RSAKeyGenParameterSpec rsaKeyGenParameterSpec =
          new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
      generator.initialize(rsaKeyGenParameterSpec);
      rsaKey = generator.generateKeyPair();
    }
  }

  // HKDF
  private void prepareHKDF() throws Exception {
    KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
    InputStream readStream = new FileInputStream(config.keyStore);
    keyStore.load(readStream, config.pass());
    readStream.close();
    hmacKey = keyStore.getKey("hmacKey", config.pass());
    hkdf = new Hkdf(provider);

    hkdf.expand((SecretKey) hmacKey, "hello world".getBytes(), 16);
  }

  public static void doHKDF(Crypto instance, int bytes, Tests.TestResult result) {
    instance._doHKDF(bytes, result);
  }

  private void _doHKDF(int bytes, Tests.TestResult result) {
    try {
      byte[] b = new byte[bytes];
      new Random().nextBytes(b);
      hkdf.expand((SecretKey) hmacKey, b, 16);
    } catch (Exception e) {
      result.exception = e;
    }
  }

  // AES encryption
  private void prepareAesEncryption() throws Exception {
    KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
    InputStream readStream = new FileInputStream(config.keyStore);
    keyStore.load(readStream, config.pass());
    readStream.close();

    aesKey = keyStore.getKey("aesKey", config.pass());
    Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
    aesCipher.update("4444444444444444".getBytes());
    aesCipher.doFinal();
  }

  public static void doAesEncryption(Crypto instance, int bytes, Tests.TestResult result) {
    instance._doAesEncryption(bytes, result);
  }

  private void _doAesEncryption(int bytes, Tests.TestResult result) {
    try {
      Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
      aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
      byte[] b = new byte[bytes];
      new Random().nextBytes(b);
      aesCipher.update(b);
      aesCipher.doFinal();
    } catch (Exception e) {
      result.exception = e;
    }
  }

  // RSA encryption
  public static byte[] doRsaEncryption(Crypto instance, int bytes, Tests.TestResult result) {
    return instance._doRsaEncryption(bytes, result);
  }

  private byte[] _doRsaEncryption(int bytes, Tests.TestResult result) {
    try {
      Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
      cipher.init(Cipher.ENCRYPT_MODE, rsaKey.getPublic());
      byte[] b = new byte[bytes];
      new Random().nextBytes(b);
      return cipher.doFinal(b);
    } catch (Exception e) {
      result.exception = e;
      return null;
    }
  }
}
