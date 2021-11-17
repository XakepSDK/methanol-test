package dk.xakeps.methanoltest;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.ProgressTracker;
import spark.Spark;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        try {
            run(args.length == 0);
        } finally {
            Spark.stop();
        }
    }

    private static void run(boolean track) throws IOException, InterruptedException, URISyntaxException {
        Spark.port(8000);
        Spark.post("/", (request, response) -> {
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            System.out.println("=== Parts ===");
            for (Part part : request.raw().getParts()) {
                System.out.println(part);
            }
            System.out.println("=== Headers ===");
            for (String header : request.headers()) {
                System.out.println(header + "=" + request.headers(header));
            }
            return "";
        });

        Methanol methanol = Methanol.create();

        URI uri = URI.create("http://localhost:8000/");

        URI resource = Main.class.getResource("/testFile").toURI();
        MultipartBodyPublisher nonTrackingPublisher = MultipartBodyPublisher.newBuilder()
                .textPart("name", "name")
                .filePart("file", Path.of(resource),
                        MediaType.APPLICATION_OCTET_STREAM)
                .build();

        HttpRequest.BodyPublisher trackingPublisher = ProgressTracker.create()
                .trackingMultipart(nonTrackingPublisher, item -> {});

        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(track ? trackingPublisher : nonTrackingPublisher)
                .build();

        methanol.send(request, HttpResponse.BodyHandlers.discarding());
    }
}
