package chronostream.common.crypto;

import chronostream.Config;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static java.lang.String.format;

public class CryptoProvider {
  private String name;
  private Provider provider;
  private Hkdf hkdf;
  private boolean allowsExport;

  public CryptoProvider(Config.CryptoProvider config) throws Exception {
    System.out.println(format("Initializing: %s", config));

    provider = (Provider) (Class.forName(config.provider()).newInstance());
    Security.addProvider(provider);

    if (provider.getClass().getName().equals("com.safenetinc.luna.provider.LunaProvider")) {
      // Luna's keystore is slightly different from other providers. The keystore file proxies the login
      // state in some cases, but not always. It's better to use the login() method on LunaSlotManager.
      // We don't want to depend on Luna's provider jar, so we use reflection.
      Class slotManagerClass = Class.forName("com.safenetinc.luna.LunaSlotManager");
      Object slotManager = slotManagerClass.getMethod("getInstance", null).invoke(null, null);
      slotManagerClass.getMethod("login", String.class).invoke(slotManager, config.password());
    }

    // Some helpful output
    Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    int maxAllowedKeyLength = aesCipher.getMaxAllowedKeyLength("AES/CBC/PKCS5Padding");
    System.out.println(
        format("Provider: %s, maxAllowedKeyLength: %d", config.name(), maxAllowedKeyLength));

    hkdf = new Hkdf(provider);

    name = config.name();
    allowsExport = config.allowsExport();
  }

  public boolean allowsExport() {
    return allowsExport;
  }

  public String getName() {
    return name;
  }

  public Object generateKey(CryptoPrimitive primitive, int keySize) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    KeyGenerator keyGen;
    KeyPairGenerator keyPairGen;

    switch (primitive) {
      case AES_CBC_ENC:
      case AES_CBC_DEC:
        keyGen = KeyGenerator.getInstance("AES", provider);
        keyGen.init(keySize);
        return keyGen.generateKey();
      case HKDF:
        String hmacAlg = "HmacSHA256";
        if (provider.getClass().getName().equals("com.safenetinc.luna.provider.LunaProvider")) {
          // Luna has a minor quirk, we have to create the hmac key as HmacSHA1.
          hmacAlg = "HmacSHA1";
        }
        keyGen = KeyGenerator.getInstance(hmacAlg, provider);
        keyGen.init(keySize);
        return keyGen.generateKey();
      case RSA_ENC:
      case RSA_DEC:
        // create RSA keys (note: JCE doesn't support rsa?)
        if (provider.getClass().getName().equals("com.sun.crypto.provider.SunJCE")) {
          return null;
        }
        keyPairGen = KeyPairGenerator.getInstance("RSA", provider);
        RSAKeyGenParameterSpec rsaKeyGenParameterSpec = new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4);
        keyPairGen.initialize(rsaKeyGenParameterSpec);
        return keyPairGen.generateKeyPair();
    }
    throw new RuntimeException("unreachable");
  }

  // HmacSha256

  public byte[] doHKDF(Object key, byte[] bytes) throws Exception {
    return hkdf.expand((SecretKey)key, bytes, 16);
  }

  // AES CBC

  public byte[] doAesCbcEncryption(Object key, byte[] bytes, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, (SecretKey)key, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  public byte[] doAesCbcDecryption(Object key, byte[] bytes, byte[] iv) throws IllegalBlockSizeException,
      BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
      NoSuchAlgorithmException, NoSuchPaddingException{
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, (SecretKey)key, new IvParameterSpec(iv));
    return cipher.doFinal(bytes);
  }

  // RSA

  public byte[] doRsaEncryption(Object keyPair, byte[] bytes) throws Exception {
    if (provider.getClass().getName().equals("com.sun.crypto.provider.SunJCE")) {
      return bytes;
    }
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
    cipher.init(Cipher.ENCRYPT_MODE, ((KeyPair)keyPair).getPublic());
    return cipher.doFinal(bytes);
  }

  public byte[] doRsaDecryption(Object keyPair, byte[] bytes) throws IllegalBlockSizeException,
      BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
    if (provider.getClass().getName().equals("com.sun.crypto.provider.SunJCE")) {
      return bytes;
    }
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", provider);
    cipher.init(Cipher.DECRYPT_MODE, ((KeyPair)keyPair).getPrivate());
    return cipher.doFinal(bytes);
  }
}
