package chronostream.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.Security;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class BouncyCastle {
  private static final String PASSWORD = "testtest";

  // Setup creates a keystore with two keys: an AES key and a RSA key.
  public static void setup() throws Exception {
    // TODO: handle the case where the keystore already exists.
    Security.addProvider(new BouncyCastleProvider());
    KeyStore keyStore = KeyStore.getInstance("JCEKS");
    keyStore.load(null, null);

    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();

    KeyStore.PasswordProtection protectionParameter =
        new KeyStore.PasswordProtection(PASSWORD.toCharArray());
    keyStore.setEntry("key1", new KeyStore.SecretKeyEntry(secretKey), protectionParameter);
    keyStore.store(new FileOutputStream("bouncycastle.jceks"), PASSWORD.toCharArray());
  }

  public static Key getAesKey() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JCEKS");
    InputStream readStream = new FileInputStream("bouncycastle.jceks");
    keyStore.load(readStream, PASSWORD.toCharArray());
    readStream.close();
    return keyStore.getKey("key1", PASSWORD.toCharArray());
  }
}
