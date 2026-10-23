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

    /** Blad z czytelnym komunikatem (bez surowego "HTTP 404"). */
    public static final class WebException extends IOException {
        WebException(String message) {
            super(message);
        }
    }

    private static String friendly(int code) {
        if (code == 404) return L.t("Not found (the file or list is not published yet).");
        if (code == 401 || code == 403) return L.t("Access denied.");
        if (code == 429) return L.t("Too many requests. Try again in a minute.");
        if (code >= 500) return L.t("The server has a problem. Try again later.");
        return L.t("Connection error.") + " (" + code + ")";
    }

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
            if (r.statusCode() / 100 != 2) throw new WebException(friendly(r.statusCode()));
            return r.body();
        } catch (WebException e) {
            throw e;
        } catch (IOException e) {
            throw new WebException(L.t("No internet connection or the server is unreachable."));
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
                throw new WebException(friendly(r.statusCode()));
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (WebException e) {
            throw e;
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw new WebException(L.t("No internet connection or the server is unreachable."));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
