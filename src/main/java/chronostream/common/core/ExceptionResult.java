package chronostream.common.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;

public final class ExceptionResult {
  private Exception exception;

  public ExceptionResult() {
    exception = null;
  }

  public void setException(Exception e) {
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
