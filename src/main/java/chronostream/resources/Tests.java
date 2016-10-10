package chronostream.resources;

import chronostream.core.Crypto;
import com.codahale.metrics.annotation.Timed;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.Validate;

import static java.lang.String.format;

@Path("/tests")
@Produces(MediaType.APPLICATION_JSON)
public class Tests {
  private final AtomicInteger testIds = new AtomicInteger();
  private Map<Integer, TestResult> testResultMap = new ConcurrentHashMap<>();

  enum Algorithms {
    AES128GCM_ENC("AES-128/GCM/NoPadding encryption",
        Crypto::prepareAesEncryption,
        Crypto::doAesEncryption),
    AES128GCM_DEC("AES-128/GCM/NoPadding decryption", null, null),
    HKDF("HKDF", null, null);

    public String name;
    public BiConsumer<Crypto, TestResult>  prepare;
    public BiConsumer<Crypto, TestResult>  run;

    Algorithms(String name, BiConsumer<Crypto, TestResult>  prepare, BiConsumer<Crypto, TestResult>  run) {
      this.name = name;
      this.prepare = prepare;
      this.run = run;
    }
  }

  public class TestResultJob {
    public long startTime;
    public long endTime;
  }

  public class TestResult {
    public List<TestResultJob> startEndTimes = Collections.synchronizedList(new ArrayList<>());
    public Exception exception;
    public int total;
  }

  public class StartResponse {
    public int id;
  }


  Map<String, Crypto> crypto;

  public Tests(Map<String, Crypto> crypto) {
    this.crypto = crypto;
  }

  public class ListResponse {
    public Map<String, String> algorithms;
    public Set<String> engines;
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
    r.algorithms = new HashMap<>();
    Arrays.stream(Algorithms.values()).forEach(e -> r.algorithms.put(e.toString(), e.name));
    r.engines = crypto.keySet();

    return r;
  }

  @GET
  @Timed
  @Path("start")
  public StartResponse start(@QueryParam("algorithm") String algorithm,
      @QueryParam("engine") String engine,
      @QueryParam("bytes") int bytes,
      @QueryParam("iterations") int iterations,
      @QueryParam("threads") int threads) {

    Algorithms alg = Algorithms.valueOf(algorithm);
    Crypto c = crypto.get(engine);

    // start test
    StartResponse r = new StartResponse();
    r.id = testIds.incrementAndGet();
    TestResult result = new TestResult();
    testResultMap.put(r.id, result);

    new Thread(new SetupTest(c, alg, result, iterations, threads)).start();

    return r;
  }

  @GET
  @Timed
  @Path("results")
  public TestResult results(@QueryParam("id") int id, @QueryParam("offset") int offset) {
    Validate.isTrue(id > 0);

    TestResult full = testResultMap.get(id);

    TestResult r = new TestResult();
    r.exception = full.exception;
    r.total = full.total;
    r.startEndTimes = new LinkedList<>();
    for (int i=offset; i<Math.min(offset+50000, full.startEndTimes.size()); i++) {
      r.startEndTimes.add(full.startEndTimes.get(i));
    }

    return r;
  }

  private class SetupTest implements Runnable {
    private Crypto crypto;
    private Algorithms alg;
    private TestResult result;
    private int iterations;
    private int threads;

    SetupTest(Crypto crypto, Algorithms alg, TestResult result, int iterations, int threads) {
      this.crypto = crypto;
      this.alg = alg;
      this.result = result;
      this.iterations = iterations;
      this.threads = threads;
    }

    public void run() {
      System.out.println(format("Starting %s with %d threads", alg.name, threads));

      try {
        alg.prepare.accept(crypto, result);

        // submit jobs
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(threads);
        ExecutorService executorService =
            new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, queue);

        for (int i=0; i<threads; i++) {
          executorService.submit(new DoTestJob(crypto, alg, result, iterations));
        }
        result.total = threads * iterations;
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        System.out.println(format("Ending test %s", alg.name));
      } catch (Exception e) {
        result.exception = e;
      }
    }
  }

  private class DoTestJob implements Runnable {
    private Crypto crypto;
    private Algorithms alg;
    private TestResult result;
    private int iterations;

    DoTestJob(Crypto crypto, Algorithms alg, TestResult result, int iterations) {
      this.crypto = crypto;
      this.alg = alg;
      this.result = result;
      this.crypto = crypto;
      this.iterations = iterations;
    }

    public void run() {
      try {
        for (int i=0; i<iterations; i++) {
          long start = System.nanoTime();
          alg.run.accept(crypto, result);
          long end = System.nanoTime();

          TestResultJob resultJob = new TestResultJob();
          resultJob.startTime = start / 1000;
          resultJob.endTime = end / 1000;
          result.startEndTimes.add(resultJob);
        }
      } catch (Exception e) {
        // TODO: improve exception handling
        System.out.println(e);
        result.exception = e;
      }
    }
  }
}
