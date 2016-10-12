package chronostream;

import chronostream.core.Crypto;
import chronostream.core.CryptoConfig;
import chronostream.resources.Dev;
import chronostream.resources.Tests;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class MyApplication extends Application<MyConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MyApplication().run(args);
    }

    @Override
    public String getName() {
        return "Chronostream";
    }

    @Override
    public void initialize(final Bootstrap<MyConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/assets/", "index.html"));
    }

    @Override
    public void run(final MyConfiguration configuration,
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
        environment.jersey().register(new Tests(crypto));
    }
}
