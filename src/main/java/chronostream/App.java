package chronostream;

import chronostream.common.crypto.Crypto;
import chronostream.common.crypto.CryptoConfig;
import chronostream.resources.Dev;
import chronostream.resources.Jobs;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

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
    public void run(final Config configuration,
                    final Environment environment) {

        // Initialize the crypto stuff
        Map<String, Crypto> crypto = new HashMap<>();
        try {
            for (CryptoConfig config : configuration.crypto) {
                crypto.put(config.name, new Crypto(config));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        environment.jersey().register(new Dev());
        environment.jersey().register(new Jobs(crypto));
    }
}
