package chronostream.perf;

import chronostream.common.core.AbstractJobResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tracks the results of performance tests.
 */
public class PerfJobResult extends AbstractJobResult {
  private long[][] results = null;
  private int total;
  private int completed;

  public void prepare(int total) {
    Objects.isNull(results);
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

  public Response getResult(int offset, int count) {
    Response r = new Response();
    r.startEndTimes = new ArrayList<>(count);
    r.exception = getException();
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
