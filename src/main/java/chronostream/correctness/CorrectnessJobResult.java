package chronostream.correctness;

import chronostream.common.core.AbstractJobResult;
import chronostream.common.crypto.Crypto;
import chronostream.common.crypto.CryptoPrimitive;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Correctness jobs need to record tuples of (Primitive, Crypto1, Crypto2).
 * For each tuple, we track how many operations succeeded and how many failed.
  */
public class CorrectnessJobResult extends AbstractJobResult {
  final Map<CryptoPrimitive, Test> results;


  public CorrectnessJobResult(int total) {
    super(total);
    results = Maps.newHashMap();
  }

  public void addResult(CryptoPrimitive primitive, Crypto enc, Crypto dec, boolean ok) {
    synchronized (results) {
      if (!results.containsKey(primitive)) {
        results.put(primitive, new Test());
      }
      Test t = results.get(primitive);
      if (!t.result.containsKey(Pair.of(enc, dec))) {
        t.result.put(Pair.of(enc, dec), new Test.Result());
      }
      if (ok) {
        t.result.get(Pair.of(enc, dec)).pass++;
      } else {
        t.result.get(Pair.of(enc, dec)).fail++;
      }
    }
  }

  public void nextIteration() {
    synchronized (results) {
      completed++;
    }
  }

  public CorrectnessJobResult getResults() throws Exception {
    CorrectnessJobResult r;
    synchronized (results) {
      r = (CorrectnessJobResult)this.clone();
    }
    return r;
  }

  static class Test {
    static class Result {
      public int pass = 0;
      public int fail = 0;
    }

    public Map<Pair<Crypto, Crypto>, Result> result = Maps.newHashMap();
  }
}
