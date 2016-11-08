package chronostream.correctness;

import chronostream.Config;
import chronostream.common.core.AbstractJobResult;
import chronostream.common.crypto.CryptoPrimitive;
import chronostream.common.crypto.CryptoProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Correctness jobs records which operations have taken place and how many times.
   */
public class CorrectnessJobResult extends AbstractJobResult {
  private List<Result> results = Lists.newArrayList();
  private AtomicInteger completed;

  public CorrectnessJobResult(Config.CorrectnessTest config, List<CryptoProvider> providers) {
    completed = new AtomicInteger();

    for (Config.Test t : config.tests()) {
      for (CryptoProvider enc : providers) {
        for (CryptoProvider dec : providers) {
          results.add(new Result(t.name(), enc.getName(), dec.getName()));
        }
      }
    }
  }

  public void nextIteration() {
    completed.incrementAndGet();
  }

  public Response getResponse() throws Exception {
    Response r = new Response();
    r.results = results;
    r.exception = getException();
    r.completed = completed.get();
    return r;
  }

  public static class Result {
    String primitive;
    String enc;
    String dec;

    public Result(String primitive, String enc, String dec) {
      this.primitive = primitive;
      this.enc = enc;
      this.dec = dec;
    }
  }

  public static class Response {
    public List<Result> results;
    public String exception;
    public int completed;
  }
}
