package chronostream.perf;

import chronostream.common.core.ExceptionResult;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the results of performance tests.
 */
public class PerfJobResult {
  private int id;
  private String job;
  private int threads;
  private int iterations;
  private int total;
  private long[][] results = null;
  private int completed;
  private ExceptionResult exception = new ExceptionResult();
  private String filename;

  public PerfJobResult(int id, String job, int threads, int iterations, int total, String filename) {
    this.id = id;
    this.job = job;
    this.threads = threads;
    this.iterations = iterations;
    this.total = total;
    this.filename = filename;
    results = new long[2][total];
    completed = 0;
  }

  public void write() throws Exception {
    //// latency: compute mean, p2, p98, q1, q2 and q3
    //long latency[] = new long[total];
    //for (int i=0; i<total; i++) {
    //  latency[i] = results[1][i] - results[0][i];
    //}
    //Arrays.sort(latency);
    //
    //long sum = 0;
    //for (int i=0; i<total; i++) {
    //  sum += latency[i];
    //}
    //float mean = (float)sum / total;
    //
    //int p2_index = (int)(Math.round(total * 0.02));
    //int p98_index = (int)(Math.round(total * 0.98));
    //int q1_index = (int)(Math.round(total * 0.25));
    //int q2_index = (int)(Math.round(total * 0.5));
    //int q3_index = (int)(Math.round(total * 0.75));
    //
    //// throughput
    //long min = results[0][0];
    //long max = results[0][1];
    //for (int i=0; i<total; i++) {
    //  if (results[0][i] < min) {
    //    min = results[0][i];
    //  }
    //  if (results[1][i] > max) {
    //    max = results[1][i];
    //  }
    //}
    //float throughput = (float)total * 1000 / (max - min);
    //
    //// write summary to log
    //ps.println(String.format("%d, %s, %d, %d, %d, "
    //        + "%d, %d, %f, %d, %d, %d, %f",
    //    id, job, threads, iterations, total,
    //    latency[p2_index], latency[p98_index], mean, latency[q1_index], latency[q2_index], latency[q3_index], throughput));

    // Log the latency
    PrintStream ps = new PrintStream(new FileOutputStream(String.format("%s-%d-%s-latency.log", filename, id, job)));
    ps.println("threads, iterations, total, latency");
    for (int i=0; i<total; i++) {
      ps.println(String.format("%d, %d, %d, %d", threads, iterations, total, results[1][i] - results[0][i]));
    }
    ps.close();

    // Log the throughput
    long min = results[0][0];
    long max = results[1][0];
    for (int i=0; i<total; i++) {
      if (results[0][i] < min) {
        min = results[0][i];
      }
      if (results[1][i] > max) {
        max = results[1][i];
      }
    }
    min = min / 1000;
    max = max / 1000;
    ps = new PrintStream(new FileOutputStream(String.format("%s-%d-%s-throughput.log", filename, id, job)));
    ps.println("threads, iterations, total, throughput");
    for (long t=min; t<=max; t++) {
      int c = 0;
      for (int i=0; i<total; i++) {
        if ((results[1][i] / 1000) == t) {
          c++;
        }
      }
      ps.println(String.format("%d, %d, %d, %d", threads, iterations, total, c));
    }
    ps.close();
  }

  public void addResult(long start, long end) {
    synchronized (results) {
      results[0][completed] = start;
      results[1][completed] = end;
      completed++;
    }
  }

  public void recordException(Exception e) {
    exception.setException(e);
  }

  public Response getResponse(int offset, int count) {
    Response r = new Response();
    r.description = String.format("%s with %d threads and %d iterations",
        job, threads, iterations);
    r.startEndTimes = new ArrayList<>(count);
    r.exception = exception.getException();
    r.total = total;
    synchronized (results) {
      r.completed = completed;
      for (int i=offset; i<Math.min(offset+count, completed); i++) {
        r.startEndTimes.add(new Response.Test(results[0][i], results[1][i]));
      }
    }
    return r;
  }

  public static class Response {
    static class Test {
      public long startTime; // in ms
      public long endTime;   // in ms

      Test(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
      }
    }
    public String description;
    public List<Test> startEndTimes;
    public String exception;
    public int completed;
    public int total;
  }
}
