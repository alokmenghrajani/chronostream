package chronostream.correctness;

import chronostream.common.core.AbstractJobResult;
import chronostream.common.crypto.Crypto;
import chronostream.common.crypto.CryptoPrimitive;
import chronostream.perf.PerfJobResult;
import com.google.common.collect.Maps;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  public Response getResults() throws Exception {
    Response r = new Response();
    synchronized (results) {
      r.total = total;
      r.completed = completed;
      if (exception == null) {
        r.exception = "";
      } else {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        exception.printStackTrace(printWriter);
        r.exception = writer.toString();
      }
      for (Map.Entry<CryptoPrimitive, Test> entry : this.results.entrySet()) {
        Map<String, Test.Result> t = Maps.newHashMap();
        for (Map.Entry<Pair<Crypto, Crypto>, Test.Result> entry2 : entry.getValue().result.entrySet()) {
          String key;
          if (entry.getKey().equals(CryptoPrimitive.HKDF)) {
            key = String.format("%s", entry2.getKey().getLeft().config.name);
          } else {
            key = String.format("%s -> %s", entry2.getKey().getLeft().config.name,
                entry2.getKey().getRight().config.name);
          }
          t.put(key, entry2.getValue());
        }
        r.results.put(entry.getKey().name, t);
      }
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

  public static class Response {
    // TODO: this is junk. I should just define how Crypto objects get converted to json.
    public Map<String, Map<String, Test.Result>> results = Maps.newHashMap();
    public String exception;
    public int completed;
    public int total;
  }
}
