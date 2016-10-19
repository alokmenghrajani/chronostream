package chronostream.perf;

import chronostream.common.core.AbstractJobResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the results of performance tests.
 */
public class PerfJobResult extends AbstractJobResult {
  final private long[][] results;

  public PerfJobResult(int total) {
    super(total);
    results = new long[2][total];
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
    if (exception == null) {
      r.exception = "";
    } else {
      Writer writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      exception.printStackTrace(printWriter);
      r.exception = writer.toString();
    }
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
