package com.wowexplorer.character;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads a character render PNG and measures the character's vertical extent by
 * scanning the alpha channel. The render is a large, mostly-transparent canvas; the
 * non-transparent rows are the character, and where they sit varies by race/model.
 *
 * <p>Best-effort: any failure (network, decode, fully transparent) returns null, and
 * the UI falls back to a fixed scale.
 */
@Component
public class RenderBoundsService {

    private static final Logger log = LoggerFactory.getLogger(RenderBoundsService.class);
    private static final int ALPHA_THRESHOLD = 16; // ignore near-transparent edge pixels

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public RenderBounds analyze(String renderUrl) {
        if (renderUrl == null || renderUrl.isBlank()) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(renderUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "wow-explorer")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.info("Render fetch for bounds returned {} ({})", response.statusCode(), renderUrl);
                return null;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (image == null) return null;

            int w = image.getWidth();
            int h = image.getHeight();
            int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);

            int minY = -1;
            int maxY = -1;
            for (int y = 0; y < h; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    if ((pixels[row + x] >>> 24) > ALPHA_THRESHOLD) {
                        if (minY < 0) minY = y;
                        maxY = y;
                        break; // this row has the character; move on
                    }
                }
            }
            if (minY < 0) return null; // fully transparent

            double top = round(minY / (double) h);
            double height = round((maxY - minY + 1) / (double) h);
            return new RenderBounds(top, height);
        } catch (Exception e) {
            log.info("Could not analyze render bounds for {}: {}", renderUrl, e.toString());
            return null;
        }
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
