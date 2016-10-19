package chronostream.perf;

import chronostream.common.core.AbstractJob;

/**
 * Schedules a perf job and gathers results.
 */
public class PerfJob extends AbstractJob<PerfJobConfig, PerfJobResult> {
  public PerfJob(PerfJobConfig config) {
    super(config);
  }

  @Override protected PerfJobResult initResult() {
    return new PerfJobResult(config.iterations * config.threads);
  }

  @Override protected Runnable initTask() {
    return new PerfJobTask(config, result);
  }
}
