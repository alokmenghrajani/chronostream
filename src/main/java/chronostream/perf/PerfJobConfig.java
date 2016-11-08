package chronostream.perf;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import java.util.Random;

import static chronostream.common.crypto.CryptoPrimitive.AES_CBC_DEC;
import static chronostream.common.crypto.CryptoPrimitive.AES_CBC_ENC;
import static chronostream.common.crypto.CryptoPrimitive.RSA_DEC;

public class PerfJobConfig {
  protected Config.Test config;
  protected CryptoProvider provider;
  protected Object key;

  public PerfJobConfig(Config.Test config, CryptoProvider provider) throws Exception {
    this.config = config;
    this.provider = provider;
    this.key = provider.generateKey(config.getPrimitive(), config.keySize());
  }

  public void doCrypto(PerfJobResult result) throws Exception {
    // data size
    int size = new Random().nextInt(config.maxDataSize() - config.minDataSize() + 1)
        + config.minDataSize();
    byte[] dataBuffer1 = new byte[size];
    new Random().nextBytes(dataBuffer1);

    byte[] iv = null;
    if (config.getPrimitive() == AES_CBC_ENC ||
        config.getPrimitive() == AES_CBC_DEC) {
        // we exclude the time it takes to generate the IV. If we decide to generate the IV
        // on the HSM, our perf numbers will be much smaller.
        iv = new byte[16];
        new Random().nextBytes(iv);
    }

    // For decryption operations, we need to first create some ciphertext
    byte[] dataBuffer2 = dataBuffer1;
    if (config.getPrimitive() == AES_CBC_DEC) {
      dataBuffer2 = provider.doAesCbcEncryption(key, dataBuffer1, iv);
    } else if (config.getPrimitive() == RSA_DEC) {
      dataBuffer2 = provider.doRsaEncryption(key, dataBuffer1);
    }

    long start = System.nanoTime();
    switch (config.getPrimitive()) {
      case AES_CBC_ENC:
        provider.doAesCbcEncryption(key, dataBuffer2, iv);
        break;
      case AES_CBC_DEC:
        provider.doAesCbcDecryption(key, dataBuffer2, iv);
        break;
      case HKDF:
        provider.doHKDF(key, dataBuffer2);
        break;
      case RSA_ENC:
        try {
          provider.doRsaEncryption(key, dataBuffer2);
        } catch (Exception e) {
          throw e;
        }
        break;
      case RSA_DEC:
        provider.doRsaDecryption(key, dataBuffer2);
        break;
    }
    long end = System.nanoTime();

    result.addResult(start/1000000, end/1000000);
  }
}
