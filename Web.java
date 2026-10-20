package com.hexmod.module;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** Proste zapytania HTTP (wywoluj poza watkiem gry). */
public final class Web {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private Web() {}

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "Hexmod/0.1 (minecraft mod)")
                .GET()
                .build();
    }

    public static String get(String url) throws IOException {
        try {
            HttpResponse<String> r = HTTP.send(request(url), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) throw new IOException("HTTP " + r.statusCode());
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    public static void download(String url, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        try {
            HttpResponse<Path> r = HTTP.send(request(url), HttpResponse.BodyHandlers.ofFile(tmp));
            if (r.statusCode() / 100 != 2) {
                Files.deleteIfExists(tmp);
                throw new IOException("HTTP " + r.statusCode());
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
