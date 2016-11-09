package chronostream.perf;

import chronostream.common.core.ExceptionResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the results of performance tests.
 */
public class PerfJobResult {
  private long[][] results = null;
  private int total;
  private int completed;
  private ExceptionResult exception = new ExceptionResult();

  public void prepare(int total) {
    if (results != null) {
      throw new RuntimeException("results should have been cleared");
    }
    results = new long[2][total];
    this.total = total;
    completed = 0;
  }

  public void flush() {
    // write results to disk

    // reset results to null.
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
        r.startEndTimes.add(new Response.Test(results[0][i],
            results[1][i]));
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
