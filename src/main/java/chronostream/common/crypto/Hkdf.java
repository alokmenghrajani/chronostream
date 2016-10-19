package chronostream.common.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import static java.util.Objects.requireNonNull;

/**
 * HMAC-based Extract-and-Expand Key Derivation Function (HKDF) from
 * <a href="http://tools.ietf.org/html/rfc5869">RFC-5869</a>. HKDF is a standard means to generate
 * a derived key of arbitrary length.
 *
 * <pre>{@code
 * // Instantiate an Hkdf object with a hash function.
 * Hkdf hkdf = new Hkdf(Hash.SHA256);
 *
 * // Using some protected input keying material (IKM), extract a pseudo-random key (PRK) with a
 * // random salt. Remember to store the salt so the key can be derived again.
 * SecretKey prk = hkdf.extract(Hkdf.randomSalt(), ikm);
 *
 * // Expand the prk with some information related to your data and the length of the output key.
 * SecretKey derivedKey = hkdf.expand(prk, "id: 5".getBytes(StandardCharsets.UTF_8), 32);
 * }</pre>
 *
 * HKDF is a generic means for generating derived keys. In some cases, you may want to use it in a
 * different manner. Consult the RFC for security considerations, when to omit a salt, skipping the
 * extraction step, etc.
 */
public class Hkdf {
  private final Provider provider;

  public Hkdf(Provider provider) {
    this.provider = provider;
  }

  /**
   * HKDF-Expand(PRK, info, L) -&gt; OKM
   *
   * @param key a pseudorandom key of at least HashLen bytes (usually, the output from the extract step)
   * @param info context and application specific information (can be empty)
   * @param outputLength length of output keying material in bytes (&lt;= 255*HashLen)
   * @return output keying material
   */
  public byte[] expand(SecretKey key, @Nullable byte[] info, int outputLength) {
    requireNonNull(key, "key must not be null");
    if (outputLength < 1) {
      throw new IllegalArgumentException("outputLength must be positive");
    }
    int hashLen = 32;
    if (outputLength > 255 * hashLen) {
      throw new IllegalArgumentException("outputLength must be less than or equal to 255*HashLen");
    }

    if (info == null) {
      info = new byte[0];
    }

    /*
    The output OKM is calculated as follows:

      N = ceil(L/HashLen)
      T = T(1) | T(2) | T(3) | ... | T(N)
      OKM = first L bytes of T

    where:
      T(0) = empty string (zero length)
      T(1) = HMAC-Hash(PRK, T(0) | info | 0x01)
      T(2) = HMAC-Hash(PRK, T(1) | info | 0x02)
      T(3) = HMAC-Hash(PRK, T(2) | info | 0x03)
      ...
     */
    int n = (outputLength % hashLen == 0) ?
        outputLength / hashLen :
        (outputLength / hashLen) + 1;

    byte[] hashRound = new byte[0];

    ByteBuffer generatedBytes = ByteBuffer.allocate(Math.multiplyExact(n, hashLen));
    Mac mac = initMac(key);
    for (int roundNum = 1; roundNum <= n; roundNum++) {
      mac.reset();
      mac.update(hashRound);
      mac.update(info);
      mac.update((byte) roundNum);
      hashRound = mac.doFinal();
      generatedBytes.put(hashRound);
    }

    byte[] result = new byte[outputLength];
    generatedBytes.rewind();
    generatedBytes.get(result, 0, outputLength);
    return result;
  }

  private Mac initMac(SecretKey key) {
    Mac mac;
    try {
      mac = Mac.getInstance("HmacSHA256", provider);
      mac.init(key);
      return mac;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeyException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
