package chronostream.correctness;

import chronostream.common.core.AbstractJob;

/**
 * Schedules a correctness job and gathers results.
 */
public class CorrectnessJob extends AbstractJob<CorrectnessJobConfig, CorrectnessJobResult> {
  public CorrectnessJob(CorrectnessJobConfig config) {
    super(config);
  }

  @Override protected CorrectnessJobResult initResult() {
    return new CorrectnessJobResult(config.iterations * config.threads);
  }

  @Override protected Runnable initTask() {
    return new CorrectnessJobTask(config, result);
  }

}
