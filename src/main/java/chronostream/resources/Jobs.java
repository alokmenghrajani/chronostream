package chronostream.resources;

import chronostream.common.core.AbstractJobResult;
import chronostream.common.crypto.Crypto;
import chronostream.common.crypto.CryptoPrimitive;
import chronostream.correctness.CorrectnessJob;
import chronostream.correctness.CorrectnessJobConfig;
import chronostream.correctness.CorrectnessJobResult;
import chronostream.perf.PerfJob;
import chronostream.perf.PerfJobConfig;
import chronostream.perf.PerfJobResult;
import com.codahale.metrics.annotation.Timed;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.Validate;

import static java.lang.String.format;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class Jobs {
  private final AtomicInteger testIds = new AtomicInteger();
  private Map<Integer, AbstractJobResult> testResultMap = new ConcurrentHashMap<>();
  Map<String, Crypto> crypto;

  public Jobs(Map<String, Crypto> crypto) {
    this.crypto = crypto;
  }

  /**
   * Returns list of algorithms & providers. Used to dynamically populate the form which
   * initiates tests.
   */
  @GET
  @Timed
  @Path("list")
  public ListResponse list() {
    ListResponse r = new ListResponse();
    r.primitives = new LinkedHashMap<>(); // Use order preserving map
    Arrays.stream(CryptoPrimitive.values()).forEach(e -> r.primitives.put(e.toString(), e.name));
    r.providers = crypto.keySet();

    return r;
  }

  /**
   * Kicks off a correctness job run.
   *
   * For HKDF, we ensure every engine gives the same output.
   * For encryption/decryption, we ensure that every engine's encryption can be decrypted
   * with every engine's decryption.
   *
   * @param iterations
   * @param threads
   */
  @POST
  @Timed
  @Path("startCorrectness")
  public StartResponse startCorrectness(
      @FormParam("iterations") int iterations,
      @FormParam("threads") int threads) {

    try {
      // start job
      StartResponse r = new StartResponse();
      r.id = testIds.incrementAndGet();
      r.summary = format("correctness (%d iterations, %d threads)", iterations, threads);

      CorrectnessJobConfig config = new CorrectnessJobConfig(new ArrayList<>(crypto.values()), iterations, threads);
      CorrectnessJob job = new CorrectnessJob(config);
      testResultMap.put(r.id, job.getResult());
      new Thread(job).start();

      return r;
    } catch (Exception e) {
      Writer writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      e.printStackTrace(printWriter);
      throw new WebApplicationException(writer.toString());
    }
  }

  @POST
  @Timed
  @Path("startPerf")
  public StartResponse startPerf(
      @FormParam("primitive") String primitiveName,
      @FormParam("provider") String provider,
      @FormParam("bytes") int bytes,
      @FormParam("iterations") int iterations,
      @FormParam("threads") int threads) {

    try {
      Crypto crypto = this.crypto.get(provider);
      CryptoPrimitive primitive = CryptoPrimitive.valueOf(primitiveName);

      // start test
      StartResponse r = new StartResponse();
      r.id = testIds.incrementAndGet();
      r.summary = format("perf %s using %s (%d bytes, %d iterations, %d threads)",
          primitive.name, provider, bytes, iterations, threads);

      PerfJobConfig config = new PerfJobConfig(crypto, primitive, bytes, iterations, threads);
      PerfJob job = new PerfJob(config);
      testResultMap.put(r.id, job.getResult());
      new Thread(job).start();
      return r;
    } catch (Exception e) {
      Writer writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      e.printStackTrace(printWriter);
      throw new WebApplicationException(writer.toString());
    }
  }

  @GET
  @Timed
  @Path("correctnessResult")
  public CorrectnessJobResult.Response correctnessResult(@QueryParam("id") int id) throws Exception {
    Validate.isTrue(id > 0);

    CorrectnessJobResult r = (CorrectnessJobResult) testResultMap.get(id);
    return r.getResults();
  }

  @GET
  @Timed
  @Path("perfResult")
  public PerfJobResult.Response perfResults(@QueryParam("id") int id, @QueryParam("offset") int offset, @QueryParam("count") int count) {
    Validate.isTrue(id > 0);

    PerfJobResult r = (PerfJobResult) testResultMap.get(id);
    return r.getResult(offset, count);
  }

  public class ListResponse {
    public Map<String, String> primitives;
    public Set<String> providers;
  }

  public class StartResponse {
    public int id;
    public String summary;
  }
}
