package chronostream.common.core;

import java.util.Objects;

/**
 *  A job results contains a progress, exceptions. Subclasses are responsible for
 *  tracking and returning results.
 */
public class AbstractJobResult {
  protected int total;
  protected int completed;
  protected Exception exception;

  public AbstractJobResult(int total) {
    this.total = total;
    completed = 0;
    exception = null;
  }

  public void recordException(Exception e) {
    Objects.requireNonNull(e);

    if (exception == null) {
      // for now, we only record the first exception.
      exception = e;
    }
  }
}
