package chronostream.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/index")
@Produces(MediaType.TEXT_HTML)
public class Index {
  @GET
  @Timed
  public String getIndex() {
    File f = new File("src/main/resources/assets/index.html");
    try {
      List<String> lines =
          Files.readAllLines(f.toPath(), Charsets.UTF_8);
      return String.join("\n", lines);
    } catch (IOException e) {
      return String.format("<html><body>%s</body></html>", e);
    }
  }
}
