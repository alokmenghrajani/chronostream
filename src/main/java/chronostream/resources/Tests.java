package chronostream.resources;

import chronostream.core.Crypto;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
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


  Map<String, Crypto> crypto;

  public Tests(Map<String, Crypto> crypto) {
    this.crypto = crypto;
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
    if (!crypto.containsKey(provider)) {
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
    new Thread(new AesGcmNoPaddingTest(crypto.get(provider), result, threads)).start();

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
    private Crypto crypto;
    private TestResult result;
    private int threads;

    AesGcmNoPaddingTest(Crypto crypto, TestResult result, int threads) {
      this.crypto = crypto;
      this.result = result;
      this.threads = threads;
    }

    public void run() {
      System.out.println(format("Starting AesGcmNoPaddingTest with %d threads", threads));

      try {
        crypto.prepareAesEncryption();

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
            executorService.submit(new AesGcmNoPaddingTestJob(clock, result, crypto));
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
    private Crypto crypto;

    AesGcmNoPaddingTestJob(Clock clock, TestResult result, Crypto crypto) {
      this.clock = clock;
      this.result = result;
      this.crypto = crypto;
    }

    public void run() {
      try {
        long start = System.nanoTime();
        crypto.doAesEncryption();
        long end = System.nanoTime();

        TestResultJob resultJob = new TestResultJob();
        resultJob.startTime = start / 1000;
        resultJob.endTime = end / 1000;
        result.startEndTimes.add(resultJob);
      } catch (Exception e) {
        // TODO: record this exception!
        System.out.println(e);
      }
    }
  }
}
