package chronostream.common.crypto;

import com.google.common.collect.ImmutableList;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import static java.lang.String.format;

public class Crypto {
  public CryptoConfig config;
  private Provider provider;
  private Hkdf hkdf;
  private SecretKey hmacKey;
  private SecretKey aes128Key, aes256Key;
  private KeyPair rsa1024Key, rsa2048Key;

  public Crypto(CryptoConfig config) throws Exception {
    this.config = config;
    System.out.println(format("Initializing: %s", config));

    provider = (Provider)(Class.forName(config.provider()).newInstance());
    Security.addProvider(provider);

    KeyStore keyStore = KeyStore.getInstance(config.storeType(), provider);

    String hmacAlg = "HmacSHA256";
    if (provider.getClass().getName().equals("com.safenetinc.luna.provider.LunaProvider")) {
      // Luna's keystore is slightly different from other providers. The keystore file proxies the login
      // state in some cases, but not always. It's better to use the login() method on LunaSlotManager.
      // We don't want to depend on Luna's provider jar, so we use reflection.
      Class slotManagerClass = Class.forName("com.safenetinc.luna.LunaSlotManager");
      Object slotManager = slotManagerClass.getMethod("getInstance", null).invoke(null, null);
      slotManagerClass.getMethod("login", String.class).invoke(slotManager, config.password());

      // Luna has a minor quirk, we have to create the hmac key as HmacSHA1.
      hmacAlg = "HmacSHA1";
    }

    // create an empty keystore
    keyStore.load(null, config.pass());

    // In some cases, the keystore isn't really empty (if the Provider decides to load keys from the HSM).
    List<String> keysToDelete = ImmutableList.of("hmacKey", "aes128Key", "aes256Key",
        "rsa1024Key", "rsa2048Key", "importedHmacKey", "importedAes128Key", "importedAes256Key",
        "importedRsa1024Key", "importedRsa2048Key");
    for (String keyToDelete: keysToDelete) {
      if (keyStore.containsAlias(keyToDelete)) {
        keyStore.deleteEntry(keyToDelete);
      }
    }

    // save the keys so we can confirm they exist/persisted
    PasswordProtection protectionParameter = new PasswordProtection(config.pass());

    // create a HmacSHA256 key, 32 bytes
    hkdf = new Hkdf(provider);
    KeyGenerator keyGen = KeyGenerator.getInstance(hmacAlg, provider);
    keyGen.init(256);
    hmacKey = keyGen.generateKey();
    keyStore.setEntry("hmacKey", new KeyStore.SecretKeyEntry(hmacKey), protectionParameter);

    // create AES keys
    Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    int maxAllowedKeyLength = aesCipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding");
    System.out.println(format("Provider: %s, maxAllowedKeyLength: %d", config.name(), maxAllowedKeyLength));

    keyGen = KeyGenerator.getInstance("AES", provider);
    keyGen.init(128);
    aes128Key = keyGen.generateKey();
    keyStore.setEntry("aes128Key", new KeyStore.SecretKeyEntry(aes128Key), protectionParameter);

    keyGen = KeyGenerator.getInstance("AES", provider);
    keyGen.init(256);
    aes256Key = keyGen.generateKey();
    keyStore.setEntry("aes256Key", new KeyStore.SecretKeyEntry(aes256Key), protectionParameter);

    // create RSA keys (note: JCE doesn't support rsa?)
    if (!provider.getClass().getName().equals("com.sun.crypto.provider.SunJCE")) {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
      RSAKeyGenParameterSpec rsaKeyGenParameterSpec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
      generator.initialize(rsaKeyGenParameterSpec);
      rsa1024Key = generator.generateKeyPair();

      generator = KeyPairGenerator.getInstance("RSA", provider);
      rsaKeyGenParameterSpec = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
      generator.initialize(rsaKeyGenParameterSpec);
      rsa2048Key = generator.generateKeyPair();

      keyStore.setEntry("rsa1024Key", new KeyStore.PrivateKeyEntry(rsa1024Key.getPrivate(), new Certificate[]{generateCertificate(rsa1024Key)}), protectionParameter);
      keyStore.setEntry("rsa2048Key", new KeyStore.PrivateKeyEntry(rsa2048Key.getPrivate(), new Certificate[]{generateCertificate(rsa2048Key)}), protectionParameter);
    }

    // save the keystore
    keyStore.store(new FileOutputStream(config.keyStore()), config.pass());
  }

  public X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
    X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
    cert.setSerialNumber(BigInteger.valueOf(1));
    cert.setSubjectDN(new X509Principal("CN=localhost"));
    cert.setIssuerDN(new X509Principal("CN=localhost"));
    cert.setPublicKey(keyPair.getPublic());
    LocalDate now = LocalDate.now();
    cert.setNotBefore(Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    now = now.plusDays(300);
    cert.setNotAfter(Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    cert.setSignatureAlgorithm("SHA512withRSA");
    PrivateKey signingKey = keyPair.getPrivate();
    return cert.generate(signingKey, provider.getName());
  }

  /**
   * Alternate constructor used for key-import.
   */
  public Crypto(CryptoConfig config, Crypto importFrom) throws Exception {
    this.config = config;
    System.out.println(format("re-initializing: %s", config));

    // create Provider
    provider = (Provider)(Class.forName(config.provider()).newInstance());
    hkdf = new Hkdf(provider);

    String hmacAlg = "HmacSHA256";
    if (provider.getClass().getName().equals("com.safenetinc.luna.provider.LunaProvider")) {
      // Luna's keystore is slightly different from other providers. The keystore file proxies the login
      // state in some cases, but not always. It's better to use the login() method on LunaSlotManager.
      // We don't want to depend on Luna's provider jar, so we use reflection.
      Class slotManagerClass = Class.forName("com.safenetinc.luna.LunaSlotManager");
      Object slotManager = slotManagerClass.getMethod("getInstance", null).invoke(null, null);
      slotManagerClass.getMethod("login", String.class).invoke(slotManager, config.password());

      // Luna has a minor quirk, we have to create the hmac key as HmacSHA1.
      hmacAlg = "HmacSHA1";
    }

    this.hmacKey = new SecretKeySpec(importFrom.hmacKey.getEncoded(), hmacAlg);
    this.aes128Key = new SecretKeySpec(importFrom.aes128Key.getEncoded(), "AES");
    this.aes256Key = new SecretKeySpec(importFrom.aes256Key.getEncoded(), "AES");
    this.rsa1024Key = importFrom.rsa1024Key;
    this.rsa2048Key = importFrom.rsa2048Key;
  }

  public byte[] doCrypto(CryptoPrimitive primitive, byte[] buffer, byte[] iv) throws Exception {
    switch (primitive) {
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

  // HmacSha256

  public byte[] doHKDF(byte[] bytes) throws Exception {
    return hkdf.expand(hmacKey, bytes, 16);
  }

  // AES CBC

  public byte[] doAesCbcEncryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, aes256Key, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  public byte[] doAesCbcDecryption(byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, aes256Key, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  // RSA

  public byte[] doRsaEncryption(byte[] bytes) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, rsa2048Key.getPublic());
    return cipher.doFinal(bytes);
  }

  public byte[] doRsaDecryption(byte[] bytes) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, rsa2048Key.getPrivate());
    return cipher.doFinal(bytes);
  }
}
