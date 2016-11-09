package chronostream.correctness;

import chronostream.Config;
import chronostream.common.core.ExceptionResult;
import chronostream.common.crypto.CryptoProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Correctness jobs records which operations have taken place and how many times.
   */
public class CorrectnessJobResult {
  private Map<ResultKey, ExceptionResult> results = Maps.newLinkedHashMap();
  private AtomicInteger completed;

  public CorrectnessJobResult(Config.CorrectnessTest config, List<CryptoProvider> providers) {
    completed = new AtomicInteger();

    for (Config.Test t : config.tests()) {
      for (CryptoProvider enc : providers) {
        for (CryptoProvider dec : providers) {
          results.put(ResultKey.create(t.name(), enc.getName(), dec.getName()), new ExceptionResult());
        }
      }
    }
  }

  public void recordException(Config.Test t, CryptoProvider enc, CryptoProvider dec, Exception e) {
    results.get(ResultKey.create(t.name(), enc.getName(), dec.getName())).setException(e);
  }

  public void nextIteration() {
    completed.incrementAndGet();
  }

  public Response getResponse() throws Exception {
    Map<String, String> r = Maps.newLinkedHashMap();
    for (Map.Entry<ResultKey, ExceptionResult> e : results.entrySet()) {
      r.put(e.getKey().toString(), e.getValue().getException());
    }
    return Response.create(r, completed.get());
  }

  @AutoValue
  abstract public static class ResultKey {
    abstract String primitive();
    abstract String enc();
    abstract String dec();

    @SuppressWarnings("unused")
    static ResultKey create(@JsonProperty("primitive") String primitive,
        @JsonProperty("enc") String enc,
        @JsonProperty("dec") String dec) {
      return new AutoValue_CorrectnessJobResult_ResultKey(primitive, enc, dec);
    }

    public String toString() {
      return String.format("%s-%s-%s", primitive(), enc(), dec());
    }
  }

  @AutoValue
  abstract public static class Response {
    @JsonProperty("results")
    abstract public Map<String, String> results();

    @JsonProperty("completed")
    abstract public int completed();

    @SuppressWarnings("unused")
    static Response create(Map<String, String> results, int completed) {
      return new AutoValue_CorrectnessJobResult_Response(results, completed);
    }
  }
}
