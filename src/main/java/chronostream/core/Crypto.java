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
  private Key aesKey;

  public Crypto(CryptoConfig config) throws Exception {
    this.config = config;
    System.out.println(format("Initializing: %s", config));

    // create Provider
    provider = (Provider)(Class.forName(config.provider).newInstance());
    Security.addProvider(provider);

    // create an empty keystore
    KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
    keyStore.load(null, config.pass());

    // create an AES key
    Cipher aesCipher = Cipher.getInstance("AES", provider);
    int maxAllowedKeyLength = aesCipher.getMaxAllowedKeyLength("AES");
    System.out.println(format("Provider: %s, maxAllowedKeyLength: %d", config.name, maxAllowedKeyLength));

    KeyGenerator keyGen = KeyGenerator.getInstance("AES", provider);
    keyGen.init(Math.min(128, maxAllowedKeyLength));
    SecretKey secretKey = keyGen.generateKey();

    // save the key
    KeyStore.PasswordProtection protectionParamter = new KeyStore.PasswordProtection(config.password.toCharArray());
    keyStore.setEntry("aesKey", new KeyStore.SecretKeyEntry(secretKey), protectionParamter);
    keyStore.store(new FileOutputStream(config.keyStore), config.password.toCharArray());
  }

  public static void prepareAesEncryption(Crypto instance, Tests.TestResult result) {
    instance._prepareAesEncryption(result);
  }

  private void _prepareAesEncryption(Tests.TestResult result) {
    try {
      KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
      InputStream readStream = new FileInputStream(config.keyStore);
      keyStore.load(readStream, config.password.toCharArray());
      readStream.close();
      aesKey = keyStore.getKey("aesKey", config.password.toCharArray());
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
      aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
      aesCipher.update("4444444444444444".getBytes());
      aesCipher.doFinal();
    } catch (Exception e) {
      result.exception = e;
    }
  }
}
