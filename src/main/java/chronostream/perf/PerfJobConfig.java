package chronostream.perf;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Random;

import static chronostream.common.crypto.CryptoPrimitive.AES_CBC_DEC;
import static chronostream.common.crypto.CryptoPrimitive.RSA_DEC;

public class PerfJobConfig {
  protected Config.Test config;
  protected CryptoProvider provider;
  private Object key;

  // We don't want the iv and ciphertext generation to hurt throughput, so we pre-compute them.
  private byte[] iv;
  private Map<Integer, byte[]> ciphertexts = Maps.newHashMap();
  private Map<Integer, byte[]> plaintexts = Maps.newHashMap();

  volatile byte[] buffer;

  public PerfJobConfig(Config.Test config, CryptoProvider provider) throws Exception {
    this.config = config;
    this.provider = provider;
    this.key = provider.generateKey(config.getPrimitive(), config.keySize());

    prepare();
  }

  private void prepare() throws Exception {
    if (config.getPrimitive().iv > 0) {
      iv = new byte[config.getPrimitive().iv];
      new Random().nextBytes(iv);
    }

    for (int i=config.minDataSize(); i<=config.maxDataSize(); i++) {
      byte[] plaintext = new byte[i];
      new Random().nextBytes(plaintext);
      plaintexts.put(i, plaintext);

      if (config.getPrimitive() == AES_CBC_DEC) {
        byte[] ciphertext = provider.doAesCbcEncryption(key, plaintext, iv);
        ciphertexts.put(i, ciphertext);
      } else if (config.getPrimitive() == RSA_DEC) {
        byte[] ciphertext = provider.doRsaEncryption(key, plaintext);
        ciphertexts.put(i, ciphertext);
      }
    }
  }

  public void doCrypto(PerfJobResult result) throws Exception {
    // data size
    int size = new Random().nextInt(config.maxDataSize() - config.minDataSize() + 1)
        + config.minDataSize();


    long start = System.nanoTime();
    switch (config.getPrimitive()) {
      case AES_CBC_ENC:
        buffer = provider.doAesCbcEncryption(key, plaintexts.get(size), iv);
        break;
      case AES_CBC_DEC:
        buffer = provider.doAesCbcDecryption(key, ciphertexts.get(size), iv);
        break;
      case HKDF:
        buffer = provider.doHKDF(key, plaintexts.get(size));
        break;
      case RSA_ENC:
        buffer = provider.doRsaEncryption(key, plaintexts.get(size));
        break;
      case RSA_DEC:
        buffer = provider.doRsaDecryption(key, ciphertexts.get(size));
        break;
      default:
        throw new Exception("unreachable");
    }
    long end = System.nanoTime();
    result.addResult(start/1000000, end/1000000);
  }
}
