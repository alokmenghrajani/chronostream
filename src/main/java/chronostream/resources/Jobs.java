package chronostream.resources;

import chronostream.correctness.CorrectnessJob;
import chronostream.correctness.CorrectnessJobResult;
import chronostream.perf.PerfJob;
import chronostream.perf.PerfJobResult;
import com.codahale.metrics.annotation.Timed;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.Validate;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class Jobs {
  PerfJob perfJob;
  CorrectnessJob correctnessJob;

  public Jobs(PerfJob perfJob, CorrectnessJob correctnessJob) {
    this.perfJob = perfJob;
    this.correctnessJob = correctnessJob;
  }

  ///**
  // * Returns list of algorithms & providers. Used to dynamically populate the form which
  // * initiates tests.
  // */
  //@GET
  //@Timed
  //@Path("list")
  //public ListResponse list() {
  //  ListResponse r = new ListResponse();
  //  r.primitives = new LinkedHashMap<>(); // Use order preserving map
  //  Arrays.stream(CryptoPrimitive.values()).forEach(e -> r.primitives.put(e.toString(), e.name));
  //  r.providers = crypto.keySet();
  //
  //  return r;
  //}

/*  @POST
  @Timed
  @Path("startPerf")
  public StartResponse startPerf(
      @FormParam("primitive") String primitiveName,
      @FormParam("provider") String provider,
      @FormParam("bytes") int bytes,
      @FormParam("iterations") int iterations,
      @FormParam("threads") int threads) {

    try {
      CryptoProvider crypto = this.crypto.get(provider);
      CryptoPrimitive primitive = CryptoPrimitive.valueOf(primitiveName);

      // start test
      StartResponse r = new StartResponse();
      //r.id = testIds.incrementAndGet();
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
*/

  @GET
  @Timed
  @Path("correctnessResult")
  public CorrectnessJobResult.Response correctnessResult() throws Exception {
    return correctnessJob.getResult().getResponse();
  }

  @GET
  @Timed
  @Path("perfResult")
  public PerfJobResult.Response perfResults(@QueryParam("id") int id, @QueryParam("offset") int offset, @QueryParam("count") int count) {
    Validate.isTrue(id > 0);

    PerfJobResult r = null; //testResultMap.get(id);
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
