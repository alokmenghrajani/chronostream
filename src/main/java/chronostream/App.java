package chronostream;

import chronostream.common.crypto.CryptoProvider;
import chronostream.correctness.CorrectnessJob;
import chronostream.perf.PerfJob;
import chronostream.resources.Dev;
import chronostream.resources.Jobs;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.List;

public class App extends Application<Config> {

  public static void main(final String[] args) throws Exception {
    new App().run(args);
  }

  @Override
  public String getName() {
    return "Chronostream";
  }

  @Override
  public void initialize(final Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets", "/assets/", "index.html"));
  }

  @Override
  public void run(final Config config,
      final Environment environment) throws Exception {

    // Initialize the crypto stuff and kick off perf and correctness tests
    List<CryptoProvider> providers = Lists.newArrayList();
    try {
      for (Config.CryptoProvider provider : config.cryptoProviders) {
        CryptoProvider cryptoProvider = new CryptoProvider(provider);
        providers.add(cryptoProvider);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    PerfJob perfJob = new PerfJob(config.perfTest, providers);
    new Thread(perfJob).start();

    CorrectnessJob correctnessJob = new CorrectnessJob(config.correctnessTest, providers);
    new Thread(correctnessJob).start();

    environment.jersey().register(new Dev());
    environment.jersey().register(new Jobs(perfJob, correctnessJob));
  }
}
