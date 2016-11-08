package chronostream.common.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;

/**
 *  A job results contains a progress, exceptions. Subclasses are responsible for
 *  tracking and returning results.
 */
public class AbstractJobResult {
  private Exception exception;

  public AbstractJobResult() {
    exception = null;
  }

  public void recordException(Exception e) {
    Objects.requireNonNull(e);

    if (exception == null) {
      // for now, we only record the first exception.
      exception = e;
    }
  }

  public String getException() {
    if (exception == null) {
      return "";
    } else {
      Writer writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      exception.printStackTrace(printWriter);
      return writer.toString();
    }
  }
}
