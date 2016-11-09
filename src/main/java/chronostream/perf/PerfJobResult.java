package chronostream.perf;

import chronostream.common.core.ExceptionResult;
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
  private PrintStream ps;

  public PerfJobResult(int id, String job, int threads, int iterations, int total, PrintStream ps) {
    this.id = id;
    this.job = job;
    this.threads = threads;
    this.iterations = iterations;
    this.total = total;
    this.ps = ps;
    results = new long[2][total];
    completed = 0;
  }

  public void write() throws Exception {
    // write summary to log
    long sum = 0;
    for (int i=0; i<total; i++) {
      sum += results[1][i] - results[0][i];
    }
    float avg = (float)sum / total;

    // throughput
    long min = results[0][0];
    long max = results[0][1];
    for (int i=0; i<total; i++) {
      if (results[0][i] < min) {
        min = results[0][i];
      }
      if (results[1][i] > max) {
        max = results[1][i];
      }
    }
    float throughput = (float)total * 1000 / (max - min);

    ps.println(String.format("%d, %s, %d, %d, %d, %f, %f", id, job, threads, iterations, total, avg, throughput));

//    for (int i=0; i<total; i++) {
//      ps.println(String.format("%d,%d", results[0][i], results[1][i]));
//    }
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

  public Response getResult(int offset, int count) {
    Response r = new Response();
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
    public List<Test> startEndTimes;
    public String exception;
    public int completed;
    public int total;
  }
}
