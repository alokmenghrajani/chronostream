package chronostream.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import chronostream.core.BouncyCastle;
import java.security.Key;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
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

  @GET
  @Timed
  @Path("start")
  public StartResponse start(@QueryParam("test") String test, @QueryParam("provider") String provider, @QueryParam("thread") int threads) {
    Validate.notNull(test);
    Validate.notNull(provider);

    // Check that test exists
    List<String> tests = ImmutableList.of("aes-gcm-nopadding");
    if (!tests.contains(test)) {
      throw new RuntimeException(format("sorry, invalid test: %s", test));
    }

    // Check that the provider exists
    List<String> providers = ImmutableList.of("bouncyCastle", "java", "hsm1", "hsm2");
    if (!providers.contains(provider)) {
      throw new RuntimeException(format("sorry, invalid provider: %s", provider));
    }

    if (threads == 0) {
      threads = 100;
    }

    // start test
    StartResponse r = new StartResponse();
    r.id = testIds.incrementAndGet();
    TestResult result = new TestResult();
    testResultMap.put(r.id, result);

    // TODO: support various tests
    new Thread(new AesGcmNoPaddingTest(result, threads)).start();

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

  private class AesGcmNoPaddingTest implements Runnable {
    private TestResult result;
    private int threads;

    AesGcmNoPaddingTest(TestResult result, int threads) {
      this.result = result;
      this.threads = threads;
    }

    public void run() {
      System.out.println(format("Starting AesGcmNoPaddingTest with %d threads", threads));

      try {
        Key key = BouncyCastle.getAesKey();

        // submit jobs
        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(threads);
        ExecutorService executorService =
            new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, queue);
        Clock clock = Clock.systemDefaultZone();
        long start = clock.millis();
        // TODO: make time a variable

        int totalSubmitted = 0;
        while (clock.millis() - start < 10000) {
          if (queue.remainingCapacity() > 0) {
            executorService.submit(new AesGcmNoPaddingTestJob(clock, result, key));
            totalSubmitted++;
          }
        }
        System.out.println(format("Ending AesGcmNoPaddingTest"));
        result.total = totalSubmitted;
        executorService.awaitTermination(1, TimeUnit.MINUTES);
      } catch (Exception e) {
        result.exception = e;
      }
    }
  }

  private class AesGcmNoPaddingTestJob implements Runnable {
    private Clock clock;
    private TestResult result;
    private Key key;

    AesGcmNoPaddingTestJob(Clock clock, TestResult result, Key key) {
      this.clock = clock;
      this.result = result;
      this.key = key;
    }

    public void run() {
      try {
        long start = clock.millis();
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, key);
        aesCipher.update("4444444444444444".getBytes());
        aesCipher.doFinal();
        long end = clock.millis();

        TestResultJob resultJob = new TestResultJob();
        resultJob.startTime = start;
        resultJob.endTime = end;
        result.startEndTimes.add(resultJob);
      } catch (Exception e) {
        // TODO: record this exception!
        System.out.println(e);
      }
    }
  }
}
