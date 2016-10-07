package chronostream.core;

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

public class Crypto {
  private CryptoConfig config;
  private Provider provider;
  private Key aesKey;

  public Crypto(CryptoConfig config) throws Exception {
    this.config = config;

    // create a keystore
    provider = (Provider)(Class.forName(config.provider).newInstance());
    Security.addProvider(provider);
    KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
    keyStore.load(null, null);

    // create a AES key
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();

    KeyStore.PasswordProtection protectionParamter = new KeyStore.PasswordProtection(config.password.toCharArray());
    keyStore.setEntry("aesKey", new KeyStore.SecretKeyEntry(secretKey), protectionParamter);
    keyStore.store(new FileOutputStream(config.keyStore), config.password.toCharArray());

    aesKey = keyStore.getKey("aesKey", config.password.toCharArray());
  }

  public void prepareAesEncryption() throws Exception {
    KeyStore keyStore = KeyStore.getInstance(config.storeType, provider);
    InputStream readStream = new FileInputStream(config.keyStore);
    keyStore.load(readStream, config.password.toCharArray());
    readStream.close();
    aesKey = keyStore.getKey("aesKey", config.password.toCharArray());
  }

  public void doAesEncryption() throws Exception {
    Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
    aesCipher.update("4444444444444444".getBytes());
    aesCipher.doFinal();
  }
}
