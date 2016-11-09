package chronostream.correctness;

import chronostream.Config;
import chronostream.common.crypto.CryptoProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import static java.lang.String.format;

public class CorrectnessJobConfig {
  public Config.Test config;
  private List<CryptoProvider> providers;
  private Object key;

  public CorrectnessJobConfig(Config.Test config, List<CryptoProvider> providers) throws Exception {
    this.config = config;
    this.providers = providers;

    // Find the provider which can be used to create the key
    CryptoProvider exportFrom = null;
    for (CryptoProvider provider : providers) {
      if (provider.allowsExport()) {
        exportFrom = provider;
        break;
      }
    }
    Objects.requireNonNull(exportFrom);
    this.key = exportFrom.generateKey(config.getPrimitive(), config.keySize());

    // todo: if we don't store the key in a keystore, we can't reproduce any exception!
  }

  public void doCrypto(CorrectnessJobResult result) throws Exception {
    int size = new Random().nextInt(config.maxDataSize() - config.minDataSize() + 1) + config.minDataSize();
    byte[] buffer = new byte[size];
    new Random().nextBytes(buffer);

    switch (config.getPrimitive()) {
      case HKDF:
        doHkdf(buffer, result);
        break;
      case AES_CBC_ENC:
        doAesCbc(buffer, result);
        break;
      case RSA_ENC:
        doRsa(buffer, result);
        break;
    }
  }

  private void doHkdf(byte[] buffer, CorrectnessJobResult result) throws Exception {
    // Ensure every provider results in the same HKDF
    CryptoProvider referenceProvider = this.providers.get(0);
    byte[] ref = referenceProvider.doHKDF(key, buffer);
    for (CryptoProvider provider : this.providers) {
      byte[] r = provider.doHKDF(key, buffer);
      if (!Arrays.equals(ref, r)) {
        Exception e = new Exception(format("%s: failed between %s and %s for %s",
            config.name(),
            referenceProvider.getName(),
            provider.getName(),
            Arrays.toString(buffer)));
        result.recordException(config, referenceProvider, provider, e);
        throw e;
      }
    }
  }

  private void doAesCbc(byte[] buffer, CorrectnessJobResult result) throws Exception {
    // Ensure every provider can decrypt the result from every other provider
    for (CryptoProvider c1 : this.providers) {
      byte[] iv = new byte[16];
      new Random().nextBytes(iv);
      byte[] t = c1.doAesCbcEncryption(key, buffer, iv);
      for (CryptoProvider c2 : this.providers) {
        try {
          byte[] r = c2.doAesCbcDecryption(key, t, iv);
          if (!Arrays.equals(buffer, r)) {
            Exception e = new Exception(format("%s != %s (iv=%s)",
                Arrays.toString(buffer),
                Arrays.toString(r),
                Arrays.toString(iv)));
            result.recordException(config, c1, c2, e);
            // Don't throw e, so we don't miss tests
          }
        } catch (IllegalBlockSizeException | BadPaddingException e) {
          result.recordException(config, c1, c2, e);
        }
      }
    }
  }

  private void doRsa(byte[] buffer, CorrectnessJobResult result) throws Exception {
    // Ensure every provider can decrypt the result from every other provider
    for (CryptoProvider c1 : this.providers) {
      byte[] t = c1.doRsaEncryption(key, buffer);
      for (CryptoProvider c2 : this.providers) {
        try {
          byte[] r = c2.doRsaDecryption(key, t);
          if (!Arrays.equals(buffer, r)) {
            Exception e = new Exception(format("%s != %s",
                Arrays.toString(buffer),
                Arrays.toString(r)));
            result.recordException(config, c1, c2, e);
            // Don't throw e, so we don't miss tests
          }
        } catch (IllegalBlockSizeException | BadPaddingException e) {
          result.recordException(config, c1, c2, e);
        }
      }
    }
  }
}
