package chronostream;

import chronostream.core.BouncyCastle;
import chronostream.resources.Index;
import chronostream.resources.Tests;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

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

        try {
            BouncyCastle.setup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(final MyConfiguration configuration,
                    final Environment environment) {
        environment.jersey().register(new Index());
        environment.jersey().register(new Tests());
    }

}
